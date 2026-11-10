package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.CatalogDtos;
import ru.unirate.api.dto.ProfileDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.domain.model.UserRole;
import ru.unirate.infrastructure.persistence.entity.NotificationSettings;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.entity.User;
import ru.unirate.infrastructure.persistence.repo.NotificationSettingsRepository;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;
import ru.unirate.infrastructure.persistence.repo.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ProfileService {

    private static final Set<SubmissionStatus> ACTIVE_STATUSES = Set.of(
            SubmissionStatus.IN_PROGRESS,
            SubmissionStatus.SUBMITTED,
            SubmissionStatus.OVERDUE,
            SubmissionStatus.RETURNED
    );

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final PointLedgerRepository ledgerRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;

    public ProfileService(UserRepository userRepository,
                          StudentProfileRepository studentProfileRepository,
                          TeacherProfileRepository teacherProfileRepository,
                          PointLedgerRepository ledgerRepository,
                          SubmissionRepository submissionRepository,
                          NotificationSettingsRepository notificationSettingsRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.ledgerRepository = ledgerRepository;
        this.submissionRepository = submissionRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
    }

    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Пользователь не найден"));
    }

    public StudentProfile requireStudentProfile(Long userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                        "Профиль студента не найден"));
    }

    public TeacherProfile requireTeacherProfile(Long userId) {
        return teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                        "Профиль преподавателя не найден"));
    }

    @Transactional(readOnly = true)
    public ProfileDtos.UserMe userMe(Long userId) {
        User user = requireUser(userId);
        ProfileDtos.StudentInfo studentInfo = null;
        ProfileDtos.TeacherInfo teacherInfo = null;
        if (user.getRole() == UserRole.STUDENT) {
            StudentProfile profile = requireStudentProfile(userId);
            studentInfo = new ProfileDtos.StudentInfo(
                    profile.getId(),
                    profile.getGroup().getId(),
                    profile.getGroup().getName(),
                    profile.getGroup().getFaculty().getId(),
                    profile.getGroup().getFaculty().getName(),
                    profile.getMembershipStatus(),
                    profile.isGroupLead(),
                    profile.getAcademicRating(),
                    ledgerRepository.sumCredits(profile.getId(), PointCurrency.BUA),
                    ledgerRepository.sumCredits(profile.getId(), PointCurrency.BOA)
            );
        } else if (user.getRole() == UserRole.TEACHER) {
            TeacherProfile teacher = requireTeacherProfile(userId);
            List<CatalogDtos.GroupRef> groups = teacher.getGroups().stream()
                    .sorted(Comparator.comparing(group -> group.getName()))
                    .map(group -> new CatalogDtos.GroupRef(group.getId(), group.getName()))
                    .toList();
            teacherInfo = new ProfileDtos.TeacherInfo(
                    teacher.getId(),
                    teacher.getDepartment().getId(),
                    teacher.getDepartment().getName(),
                    groups
            );
        }
        return new ProfileDtos.UserMe(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEmailVerified(), user.getCreatedAt(), studentInfo, teacherInfo);
    }

    @Transactional(readOnly = true)
    public ProfileDtos.ProfileResponse profile(AuthUser viewer, Long targetUserId) {
        User target = requireUser(targetUserId);
        boolean canSeeDetails = viewer.id().equals(targetUserId) || viewer.isStaff();
        String email = canSeeDetails ? target.getEmail() : null;

        Integer academicRating = null;
        Long bua = null;
        Long boa = null;
        String groupName = null;
        String facultyName = null;
        String departmentName = null;
        var membershipStatus = (ru.unirate.domain.model.MembershipStatus) null;
        Boolean isGroupLead = null;
        List<ProfileDtos.LedgerEntryDto> ledger = List.of();
        List<ProfileDtos.ProfileCheckpointDto> active = List.of();
        List<ProfileDtos.ProfileCheckpointDto> completed = List.of();

        if (target.getRole() == UserRole.STUDENT) {
            StudentProfile profile = requireStudentProfile(targetUserId);
            groupName = profile.getGroup().getName();
            facultyName = profile.getGroup().getFaculty().getName();
            academicRating = profile.getAcademicRating();
            bua = ledgerRepository.sumCredits(profile.getId(), PointCurrency.BUA);
            boa = ledgerRepository.sumCredits(profile.getId(), PointCurrency.BOA);
            membershipStatus = profile.getMembershipStatus();
            isGroupLead = profile.isGroupLead();
            if (canSeeDetails) {
                ledger = mapLedger(ledgerRepository.findByStudentProfileIdOrderByIdDesc(
                        profile.getId(), org.springframework.data.domain.PageRequest.of(0, 20)));
                List<Submission> submissions = submissionRepository
                        .findByStudentProfileIdOrderByCheckpointDeadlineDesc(profile.getId());
                active = submissions.stream()
                        .filter(submission -> ACTIVE_STATUSES.contains(submission.getStatus()))
                        .map(this::toProfileCheckpoint)
                        .toList();
                completed = submissions.stream()
                        .filter(submission -> submission.getStatus() == SubmissionStatus.ACCEPTED)
                        .map(this::toProfileCheckpoint)
                        .toList();
            }
        } else if (target.getRole() == UserRole.TEACHER) {
            TeacherProfile teacher = requireTeacherProfile(targetUserId);
            departmentName = teacher.getDepartment().getName();
        }

        return new ProfileDtos.ProfileResponse(
                target.getId(),
                target.getFullName(),
                target.getRole(),
                email,
                target.isEmailVerified(),
                groupName,
                facultyName,
                departmentName,
                academicRating,
                bua,
                boa,
                membershipStatus,
                isGroupLead,
                ledger,
                active,
                completed
        );
    }

    @Transactional(readOnly = true)
    public List<ProfileDtos.LedgerEntryDto> ledger(AuthUser viewer, Long targetUserId, Long cursor, int limit) {
        if (!viewer.id().equals(targetUserId) && !viewer.isStaff()) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "История баллов доступна только владельцу профиля");
        }
        StudentProfile profile = requireStudentProfile(targetUserId);
        int pageSize = Math.max(1, Math.min(limit, 100));
        var pageable = org.springframework.data.domain.PageRequest.of(0, pageSize);
        List<PointLedgerEntry> entries = cursor == null
                ? ledgerRepository.findByStudentProfileIdOrderByIdDesc(profile.getId(), pageable)
                : ledgerRepository.findByStudentProfileIdAndIdLessThanOrderByIdDesc(profile.getId(), cursor, pageable);
        return mapLedger(entries);
    }

    private List<ProfileDtos.LedgerEntryDto> mapLedger(List<PointLedgerEntry> entries) {
        List<ProfileDtos.LedgerEntryDto> result = new ArrayList<>(entries.size());
        for (PointLedgerEntry entry : entries) {
            result.add(new ProfileDtos.LedgerEntryDto(
                    entry.getId(),
                    entry.getCreatedAt(),
                    entry.getCurrency(),
                    entry.getAmount(),
                    entry.getReason(),
                    entry.getActor() == null ? null : entry.getActor().getFullName(),
                    entry.getRefType(),
                    entry.getRefId()
            ));
        }
        return result;
    }

    private ProfileDtos.ProfileCheckpointDto toProfileCheckpoint(Submission submission) {
        return new ProfileDtos.ProfileCheckpointDto(
                submission.getCheckpoint().getId(),
                submission.getCheckpoint().getTitle(),
                submission.getCheckpoint().getDeadline(),
                submission.getStatus().name(),
                submission.getCheckpoint().getCost(),
                submission.getArDebited()
        );
    }

    @Transactional(readOnly = true)
    public ProfileDtos.NotificationSettingsDto notificationSettings(Long userId) {
        NotificationSettings settings = getOrCreateSettings(userId);
        return new ProfileDtos.NotificationSettingsDto(
                settings.isEmailEnabled(),
                settings.isTelegramEnabled(),
                settings.getTelegramChatId()
        );
    }

    @Transactional
    public ProfileDtos.NotificationSettingsDto updateNotificationSettings(
            Long userId, ProfileDtos.UpdateNotificationSettingsRequest request) {
        NotificationSettings settings = getOrCreateSettings(userId);
        settings.setEmailEnabled(request.emailEnabled());
        settings.setTelegramEnabled(request.telegramEnabled());
        settings.setTelegramChatId(request.telegramChatId() == null || request.telegramChatId().isBlank()
                ? null : request.telegramChatId().trim());
        notificationSettingsRepository.save(settings);
        return new ProfileDtos.NotificationSettingsDto(
                settings.isEmailEnabled(), settings.isTelegramEnabled(), settings.getTelegramChatId());
    }

    private NotificationSettings getOrCreateSettings(Long userId) {
        return notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> notificationSettingsRepository.save(new NotificationSettings(userId)));
    }
}
