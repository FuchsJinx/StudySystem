package ru.unirate.application.imports;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.UserRole;
import ru.unirate.domain.rating.RatingEngine;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.Faculty;
import ru.unirate.infrastructure.persistence.entity.NotificationSettings;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.FacultyRepository;
import ru.unirate.infrastructure.persistence.repo.NotificationSettingsRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.UserRepository;
import ru.unirate.application.SettingsService;

import java.util.Locale;
import java.util.regex.Pattern;

/** One imported student in its own transaction so successful rows survive bad rows. */
@Service
public class StudentImportRowProcessor {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String DEFAULT_PASSWORD = "Demo123!";

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyRepository facultyRepository;
    private final StudentGroupRepository groupRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;

    public StudentImportRowProcessor(UserRepository userRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     FacultyRepository facultyRepository,
                                     StudentGroupRepository groupRepository,
                                     NotificationSettingsRepository notificationSettingsRepository,
                                     PasswordEncoder passwordEncoder,
                                     SettingsService settingsService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyRepository = facultyRepository;
        this.groupRepository = groupRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.settingsService = settingsService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome importRow(StudentImportRow row) {
        String email = row.email() == null ? "" : row.email().trim().toLowerCase(Locale.ROOT);
        String fullName = row.fullName() == null ? "" : row.fullName().trim();
        String facultyName = row.facultyName() == null ? "" : row.facultyName().trim();
        String groupName = row.groupName() == null ? "" : row.groupName().trim();
        if (email.isEmpty() || !EMAIL.matcher(email).matches()) {
            throw new ImportRowException("Некорректный email");
        }
        if (fullName.isEmpty()) {
            throw new ImportRowException("Не указано ФИО");
        }
        if (facultyName.isEmpty() || groupName.isEmpty()) {
            throw new ImportRowException("Не указаны факультет или группа");
        }
        String password = row.password() == null || row.password().isBlank()
                ? DEFAULT_PASSWORD : row.password().trim();
        if (password.length() < 8) {
            throw new ImportRowException("Пароль должен содержать минимум 8 символов");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return Outcome.skipped("Пользователь с email " + email + " уже существует");
        }

        Faculty faculty = facultyRepository.findByNameIgnoreCase(facultyName)
                .orElseGet(() -> facultyRepository.save(new Faculty(facultyName)));
        StudentGroup group = groupRepository
                .findByFacultyIdAndNameIgnoreCase(faculty.getId(), groupName)
                .orElseGet(() -> {
                    StudentGroup created = new StudentGroup();
                    created.setName(groupName);
                    created.setFaculty(faculty);
                    return groupRepository.save(created);
                });

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(UserRole.STUDENT);
        user.setEmailVerified(true);
        userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setGroup(group);
        profile.setMembershipStatus(MembershipStatus.CONFIRMED);
        profile.setGroupLead(false);
        int arMax = settingsService.getInt(SettingsCatalog.AR_MAX);
        int startAr = settingsService.getInt(SettingsCatalog.AR_START_ON_REGISTER);
        profile.setAcademicRating(RatingEngine.clamp(startAr, arMax));
        studentProfileRepository.save(profile);

        notificationSettingsRepository.save(new NotificationSettings(user.getId()));
        return Outcome.created("Создан студент " + email);
    }

    public record Outcome(boolean created, String message) {
        public static Outcome created(String message) {
            return new Outcome(true, message);
        }

        public static Outcome skipped(String message) {
            return new Outcome(false, message);
        }
    }
}
