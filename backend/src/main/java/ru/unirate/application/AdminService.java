package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.AdminDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.model.UserRole;
import ru.unirate.domain.rating.RatingEngine;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.Department;
import ru.unirate.infrastructure.persistence.entity.Faculty;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.infrastructure.persistence.entity.Semester;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.ActivityRepository;
import ru.unirate.infrastructure.persistence.repo.AwardFeedItemRepository;
import ru.unirate.infrastructure.persistence.repo.CheckpointRepository;
import ru.unirate.infrastructure.persistence.repo.DepartmentRepository;
import ru.unirate.infrastructure.persistence.repo.FacultyRepository;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.PollRepository;
import ru.unirate.infrastructure.persistence.repo.SemesterRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;
import ru.unirate.infrastructure.persistence.repo.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final FacultyRepository facultyRepository;
    private final StudentGroupRepository groupRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CheckpointRepository checkpointRepository;
    private final ActivityRepository activityRepository;
    private final PollRepository pollRepository;
    private final AwardFeedItemRepository awardFeedItemRepository;
    private final SemesterRepository semesterRepository;
    private final PointLedgerRepository ledgerRepository;
    private final SettingsService settingsService;

    public AdminService(FacultyRepository facultyRepository,
                        StudentGroupRepository groupRepository,
                        DepartmentRepository departmentRepository,
                        UserRepository userRepository,
                        StudentProfileRepository studentProfileRepository,
                        TeacherProfileRepository teacherProfileRepository,
                        CheckpointRepository checkpointRepository,
                        ActivityRepository activityRepository,
                        PollRepository pollRepository,
                        AwardFeedItemRepository awardFeedItemRepository,
                        SemesterRepository semesterRepository,
                        PointLedgerRepository ledgerRepository,
                        SettingsService settingsService) {
        this.facultyRepository = facultyRepository;
        this.groupRepository = groupRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.checkpointRepository = checkpointRepository;
        this.activityRepository = activityRepository;
        this.pollRepository = pollRepository;
        this.awardFeedItemRepository = awardFeedItemRepository;
        this.semesterRepository = semesterRepository;
        this.ledgerRepository = ledgerRepository;
        this.settingsService = settingsService;
    }

    // ---------- Catalogs ----------

    @Transactional(readOnly = true)
    public List<AdminDtos.FacultyAdminDto> faculties() {
        return facultyRepository.findAll().stream()
                .sorted(Comparator.comparing(Faculty::getName))
                .map(faculty -> new AdminDtos.FacultyAdminDto(
                        faculty.getId(), faculty.getName(), groupRepository.countByFacultyId(faculty.getId())))
                .toList();
    }

    @Transactional
    public AdminDtos.FacultyAdminDto createFaculty(AdminDtos.NameRequest request) {
        String name = request.name().trim();
        if (facultyRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Факультет с таким названием уже существует");
        }
        Faculty faculty = facultyRepository.save(new Faculty(name));
        return new AdminDtos.FacultyAdminDto(faculty.getId(), faculty.getName(), 0);
    }

    @Transactional
    public AdminDtos.FacultyAdminDto updateFaculty(Long id, AdminDtos.NameRequest request) {
        Faculty faculty = facultyOrThrow(id);
        String name = request.name().trim();
        if (facultyRepository.existsByNameIgnoreCase(name) && !faculty.getName().equalsIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Факультет с таким названием уже существует");
        }
        faculty.setName(name);
        return new AdminDtos.FacultyAdminDto(faculty.getId(), faculty.getName(),
                groupRepository.countByFacultyId(faculty.getId()));
    }

    @Transactional
    public void deleteFaculty(Long id) {
        Faculty faculty = facultyOrThrow(id);
        if (groupRepository.countByFacultyId(id) > 0) {
            throw ApiException.conflict(ErrorCodes.CONFLICT,
                    "Нельзя удалить факультет, пока в нём есть группы");
        }
        facultyRepository.delete(faculty);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.GroupAdminDto> groups() {
        return groupRepository.findAllByOrderByNameAsc().stream()
                .map(group -> new AdminDtos.GroupAdminDto(
                        group.getId(), group.getName(),
                        group.getFaculty().getId(), group.getFaculty().getName()))
                .toList();
    }

    @Transactional
    public AdminDtos.GroupAdminDto createGroup(AdminDtos.GroupRequest request) {
        Faculty faculty = facultyOrThrow(request.facultyId());
        String name = request.name().trim();
        if (groupRepository.findByFacultyIdAndNameIgnoreCase(faculty.getId(), name).isPresent()) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Группа с таким названием уже есть на факультете");
        }
        StudentGroup group = new StudentGroup();
        group.setName(name);
        group.setFaculty(faculty);
        groupRepository.save(group);
        return new AdminDtos.GroupAdminDto(
                group.getId(), group.getName(), faculty.getId(), faculty.getName());
    }

    @Transactional
    public AdminDtos.GroupAdminDto updateGroup(Long id, AdminDtos.GroupRequest request) {
        StudentGroup group = groupOrThrow(id);
        Faculty faculty = facultyOrThrow(request.facultyId());
        String name = request.name().trim();
        groupRepository.findByFacultyIdAndNameIgnoreCase(faculty.getId(), name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.conflict(ErrorCodes.CONFLICT,
                            "Группа с таким названием уже есть на факультете");
                });
        group.setName(name);
        group.setFaculty(faculty);
        return new AdminDtos.GroupAdminDto(
                group.getId(), group.getName(), faculty.getId(), faculty.getName());
    }

    @Transactional
    public void deleteGroup(Long id) {
        StudentGroup group = groupOrThrow(id);
        if (studentProfileRepository.countByGroupId(id) > 0) {
            throw ApiException.conflict(ErrorCodes.CONFLICT,
                    "Нельзя удалить группу, пока в ней есть студенты");
        }
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.DepartmentAdminDto> departments() {
        Map<Long, Long> counts = teacherProfileRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        teacher -> teacher.getDepartment().getId(), Collectors.counting()));
        return departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Department::getName))
                .map(department -> new AdminDtos.DepartmentAdminDto(
                        department.getId(), department.getName(),
                        counts.getOrDefault(department.getId(), 0L)))
                .toList();
    }

    @Transactional
    public AdminDtos.DepartmentAdminDto createDepartment(AdminDtos.NameRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Кафедра с таким названием уже существует");
        }
        Department department = departmentRepository.save(new Department(name));
        return new AdminDtos.DepartmentAdminDto(department.getId(), department.getName(), 0);
    }

    @Transactional
    public AdminDtos.DepartmentAdminDto updateDepartment(Long id, AdminDtos.NameRequest request) {
        Department department = departmentOrThrow(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)
                && !department.getName().equalsIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.CONFLICT, "Кафедра с таким названием уже существует");
        }
        department.setName(name);
        long teachers = teacherProfileRepository.findAll().stream()
                .filter(teacher -> teacher.getDepartment().getId().equals(id)).count();
        return new AdminDtos.DepartmentAdminDto(department.getId(), department.getName(), teachers);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentOrThrow(id);
        boolean hasTeachers = teacherProfileRepository.findAll().stream()
                .anyMatch(teacher -> teacher.getDepartment().getId().equals(id));
        if (hasTeachers) {
            throw ApiException.conflict(ErrorCodes.CONFLICT,
                    "Нельзя удалить кафедру, пока к ней привязаны преподаватели");
        }
        departmentRepository.delete(department);
    }

    // ---------- Settings ----------

    @Transactional(readOnly = true)
    public List<AdminDtos.SettingDto> settings() {
        Map<String, String> values = settingsService.allValues();
        return settingsService.definitions().stream()
                .map(definition -> new AdminDtos.SettingDto(
                        definition.key(),
                        values.getOrDefault(definition.key(), definition.defaultValue()),
                        definition.title(),
                        definition.description(),
                        definition.min(),
                        definition.max(),
                        definition.unit()
                ))
                .toList();
    }

    @Transactional
    public List<AdminDtos.SettingDto> updateSettings(AdminDtos.SettingsUpdateRequest request) {
        settingsService.update(request.values());
        return settings();
    }

    // ---------- Semesters ----------

    @Transactional(readOnly = true)
    public List<AdminDtos.SemesterDto> semesters() {
        return semesterRepository.findAllByOrderByStartAtDesc().stream().map(this::toSemesterDto).toList();
    }

    @Transactional
    public AdminDtos.SemesterDto createSemester(AdminDtos.CreateSemesterRequest request) {
        if (request.endAt().isBefore(request.startAt())) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                    "Дата окончания семестра не может быть раньше даты начала");
        }
        Semester semester = new Semester();
        semester.setName(request.name().trim());
        semester.setStartAt(request.startAt());
        semester.setEndAt(request.endAt());
        semester.setActive(false);
        semesterRepository.save(semester);
        return toSemesterDto(semester);
    }

    @Transactional
    public AdminDtos.SemesterActivationDto activateSemester(Long semesterId) {
        Semester semester = semesterRepository.findForUpdate(semesterId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SEMESTER_NOT_FOUND, "Семестр не найден"));
        if (semester.getActivatedAt() != null) {
            throw ApiException.conflict(ErrorCodes.SEMESTER_ALREADY_ACTIVATED,
                    "Этот семестр уже был активирован");
        }
        for (Semester active : semesterRepository.findAll()) {
            if (active.isActive() && !active.getId().equals(semester.getId())) {
                active.setActive(false);
                semesterRepository.save(active);
            }
        }
        semester.setActive(true);
        semester.setActivatedAt(Instant.now());
        semesterRepository.save(semester);

        int bonus = settingsService.getInt(SettingsCatalog.SEMESTER_BONUS_AR);
        int arMax = settingsService.getInt(SettingsCatalog.AR_MAX);
        int affected = 0;
        int totalBonus = 0;
        List<StudentProfile> students = studentProfileRepository.findAll();
        for (StudentProfile student : students) {
            int before = student.getAcademicRating();
            int after = RatingEngine.applySemesterBonus(before, bonus, arMax);
            int delta = after - before;
            if (delta <= 0) {
                continue;
            }
            student.setAcademicRating(after);
            studentProfileRepository.save(student);

            PointLedgerEntry entry = new PointLedgerEntry();
            entry.setStudentProfile(student);
            entry.setCurrency(PointCurrency.AR);
            entry.setAmount(delta);
            entry.setReason("Семестровый бонус: " + semester.getName());
            entry.setRefType("SEMESTER");
            entry.setRefId(semester.getId());
            ledgerRepository.save(entry);

            affected++;
            totalBonus += delta;
        }
        return new AdminDtos.SemesterActivationDto(toSemesterDto(semester), affected, totalBonus);
    }

    // ---------- Users / stats ----------

    @Transactional(readOnly = true)
    public List<AdminDtos.AdminUserDto> users(String roleRaw) {
        UserRole role = null;
        if (roleRaw != null && !roleRaw.isBlank()) {
            try {
                role = UserRole.valueOf(roleRaw.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "Неизвестная роль");
            }
        }
        Map<Long, StudentProfile> students = studentProfileRepository.findAll().stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        Map<Long, TeacherProfile> teachers = teacherProfileRepository.findAll().stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        List<User> users = role == null
                ? userRepository.findAllByOrderByFullNameAsc()
                : userRepository.findAllByRoleOrderByFullNameAsc(role);
        List<AdminDtos.AdminUserDto> result = new ArrayList<>();
        for (User user : users) {
            StudentProfile student = students.get(user.getId());
            TeacherProfile teacher = teachers.get(user.getId());
            result.add(new AdminDtos.AdminUserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.isEmailVerified(),
                    student == null ? null : student.getGroup().getName(),
                    teacher == null ? null : teacher.getDepartment().getName(),
                    student == null ? null : student.getMembershipStatus(),
                    user.getCreatedAt()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AdminDtos.AdminStatsDto stats() {
        return new AdminDtos.AdminStatsDto(
                facultyRepository.count(),
                groupRepository.count(),
                departmentRepository.count(),
                studentProfileRepository.count(),
                teacherProfileRepository.count(),
                userRepository.countByRole(UserRole.ADMIN),
                studentProfileRepository.countByMembershipStatus(MembershipStatus.PENDING),
                checkpointRepository.count(),
                activityRepository.count(),
                pollRepository.count(),
                awardFeedItemRepository.count()
        );
    }

    private Faculty facultyOrThrow(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.FACULTY_NOT_FOUND, "Факультет не найден"));
    }

    private StudentGroup groupOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND, "Группа не найдена"));
    }

    private Department departmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.DEPARTMENT_NOT_FOUND, "Кафедра не найдена"));
    }

    private AdminDtos.SemesterDto toSemesterDto(Semester semester) {
        return new AdminDtos.SemesterDto(
                semester.getId(), semester.getName(), semester.getStartAt(), semester.getEndAt(),
                semester.isActive(), semester.getActivatedAt());
    }
}
