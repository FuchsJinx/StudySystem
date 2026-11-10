package ru.unirate.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.AuthDtos;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.ProfileDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.api.security.JwtService;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.UserRole;
import ru.unirate.domain.rating.RatingEngine;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.mail.MailService;
import ru.unirate.infrastructure.persistence.entity.Department;
import ru.unirate.infrastructure.persistence.entity.EmailVerificationToken;
import ru.unirate.infrastructure.persistence.entity.NotificationSettings;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.DepartmentRepository;
import ru.unirate.infrastructure.persistence.repo.EmailVerificationTokenRepository;
import ru.unirate.infrastructure.persistence.repo.NotificationSettingsRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;
import ru.unirate.infrastructure.persistence.repo.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final long VERIFICATION_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentGroupRepository groupRepository;
    private final DepartmentRepository departmentRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final SettingsService settingsService;
    private final ProfileService profileService;
    private final CatalogService catalogService;

    public AuthService(UserRepository userRepository,
                       StudentProfileRepository studentProfileRepository,
                       TeacherProfileRepository teacherProfileRepository,
                       StudentGroupRepository groupRepository,
                       DepartmentRepository departmentRepository,
                       EmailVerificationTokenRepository tokenRepository,
                       NotificationSettingsRepository notificationSettingsRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MailService mailService,
                       SettingsService settingsService,
                       ProfileService profileService,
                       CatalogService catalogService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.groupRepository = groupRepository;
        this.departmentRepository = departmentRepository;
        this.tokenRepository = tokenRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.settingsService = settingsService;
        this.profileService = profileService;
        this.catalogService = catalogService;
    }

    @Transactional
    public AuthDtos.AuthResponse registerStudent(AuthDtos.RegisterStudentRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict(ErrorCodes.EMAIL_ALREADY_USED, "Пользователь с таким email уже зарегистрирован");
        }
        StudentGroup group = catalogService.groupOrThrow(request.groupId());
        if (!group.getFaculty().getId().equals(request.facultyId())) {
            throw ApiException.badRequest(ErrorCodes.GROUP_NOT_FOUND, "Выбранная группа не относится к факультету");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(UserRole.STUDENT);
        user.setEmailVerified(false);
        userRepository.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setGroup(group);
        profile.setMembershipStatus(MembershipStatus.PENDING);
        profile.setGroupLead(false);
        int arMax = settingsService.getInt(SettingsCatalog.AR_MAX);
        int startAr = settingsService.getInt(SettingsCatalog.AR_START_ON_REGISTER);
        profile.setAcademicRating(RatingEngine.clamp(startAr, arMax));
        studentProfileRepository.save(profile);

        notificationSettingsRepository.save(new NotificationSettings(user.getId()));
        issueVerificationToken(user);
        return new AuthDtos.AuthResponse(
                jwtService.createToken(user, profile.getId(), null),
                profileService.userMe(user.getId()));
    }

    @Transactional
    public AuthDtos.AuthResponse registerTeacher(AuthDtos.RegisterTeacherRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict(ErrorCodes.EMAIL_ALREADY_USED, "Пользователь с таким email уже зарегистрирован");
        }
        Department department = catalogService.departmentOrThrow(request.departmentId());

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(UserRole.TEACHER);
        user.setEmailVerified(false);
        userRepository.save(user);

        TeacherProfile profile = new TeacherProfile();
        profile.setUser(user);
        profile.setDepartment(department);
        teacherProfileRepository.save(profile);

        notificationSettingsRepository.save(new NotificationSettings(user.getId()));
        issueVerificationToken(user);
        return new AuthDtos.AuthResponse(
                jwtService.createToken(user, null, profile.getId()),
                profileService.userMe(user.getId()));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> ApiException.unauthorized(ErrorCodes.INVALID_CREDENTIALS,
                        "Неверный email или пароль"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized(ErrorCodes.INVALID_CREDENTIALS, "Неверный email или пароль");
        }
        Long studentProfileId = studentProfileRepository.findByUserId(user.getId())
                .map(StudentProfile::getId).orElse(null);
        Long teacherProfileId = teacherProfileRepository.findByUserId(user.getId())
                .map(TeacherProfile::getId).orElse(null);
        return new AuthDtos.AuthResponse(
                jwtService.createToken(user, studentProfileId, teacherProfileId),
                profileService.userMe(user.getId()));
    }

    @Transactional
    public CommonDtos.MessageResponse verifyEmail(String token) {
        EmailVerificationToken verification = tokenRepository.findByToken(token.trim())
                .orElseThrow(() -> ApiException.badRequest(ErrorCodes.EMAIL_VERIFICATION_INVALID,
                        "Ссылка подтверждения недействительна"));
        if (verification.getUsedAt() != null) {
            throw ApiException.badRequest(ErrorCodes.EMAIL_VERIFICATION_INVALID,
                    "Ссылка подтверждения уже использована");
        }
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest(ErrorCodes.EMAIL_VERIFICATION_INVALID,
                    "Срок действия ссылки подтверждения истёк");
        }
        verification.setUsedAt(Instant.now());
        tokenRepository.save(verification);
        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        return new CommonDtos.MessageResponse("Email успешно подтверждён");
    }

    @Transactional
    public CommonDtos.MessageResponse resendVerification(AuthUser authUser) {
        User user = profileService.requireUser(authUser.id());
        if (user.isEmailVerified()) {
            throw ApiException.badRequest(ErrorCodes.EMAIL_ALREADY_VERIFIED, "Email уже подтверждён");
        }
        issueVerificationToken(user);
        return new CommonDtos.MessageResponse("Письмо с подтверждением отправлено повторно");
    }

    private void issueVerificationToken(User user) {
        tokenRepository.invalidateForUser(user.getId(), Instant.now());
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(Instant.now().plus(VERIFICATION_TTL_HOURS, ChronoUnit.HOURS));
        tokenRepository.save(token);
        mailService.sendVerificationEmail(user, token.getToken());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
