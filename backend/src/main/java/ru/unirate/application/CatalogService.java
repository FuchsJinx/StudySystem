package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.CatalogDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.infrastructure.persistence.entity.Department;
import ru.unirate.infrastructure.persistence.entity.Faculty;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.repo.DepartmentRepository;
import ru.unirate.infrastructure.persistence.repo.FacultyRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class CatalogService {

    private final FacultyRepository facultyRepository;
    private final StudentGroupRepository groupRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    public CatalogService(FacultyRepository facultyRepository,
                          StudentGroupRepository groupRepository,
                          DepartmentRepository departmentRepository,
                          TeacherProfileRepository teacherProfileRepository) {
        this.facultyRepository = facultyRepository;
        this.groupRepository = groupRepository;
        this.departmentRepository = departmentRepository;
        this.teacherProfileRepository = teacherProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.FacultyDto> faculties() {
        return facultyRepository.findAll().stream()
                .sorted(Comparator.comparing(Faculty::getName))
                .map(faculty -> new CatalogDtos.FacultyDto(faculty.getId(), faculty.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.GroupDto> groupsByFaculty(Long facultyId) {
        facultyOrThrow(facultyId);
        return groupRepository.findAllByFacultyIdOrderByNameAsc(facultyId).stream()
                .map(this::toGroupDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.GroupDto> allGroups() {
        return groupRepository.findAllByOrderByNameAsc().stream()
                .map(this::toGroupDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.DepartmentDto> departments() {
        return departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Department::getName))
                .map(department -> new CatalogDtos.DepartmentDto(department.getId(), department.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.GroupDto> myGroups(AuthUser user) {
        TeacherProfile teacher = teacherProfileRepository.findByUserId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                        "Профиль преподавателя не найден"));
        return teacher.getGroups().stream()
                .sorted(Comparator.comparing(StudentGroup::getName))
                .map(this::toGroupDto)
                .toList();
    }

    public Faculty facultyOrThrow(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.FACULTY_NOT_FOUND, "Факультет не найден"));
    }

    public StudentGroup groupOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND, "Группа не найдена"));
    }

    public Department departmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.DEPARTMENT_NOT_FOUND, "Кафедра не найдена"));
    }

    private CatalogDtos.GroupDto toGroupDto(StudentGroup group) {
        return new CatalogDtos.GroupDto(group.getId(), group.getName(),
                group.getFaculty().getId(), group.getFaculty().getName());
    }
}
