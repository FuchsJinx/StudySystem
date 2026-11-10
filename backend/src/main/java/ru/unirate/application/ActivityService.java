package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.ActivityDtos;
import ru.unirate.api.dto.CatalogDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.budget.BudgetPolicy;
import ru.unirate.domain.model.ActivityType;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.model.UserRole;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.BudgetStore;
import ru.unirate.infrastructure.persistence.entity.Activity;
import ru.unirate.infrastructure.persistence.entity.ActivityAward;
import ru.unirate.infrastructure.persistence.entity.ActivityEnrollment;
import ru.unirate.infrastructure.persistence.entity.AwardFeedItem;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.repo.ActivityAwardRepository;
import ru.unirate.infrastructure.persistence.repo.ActivityEnrollmentRepository;
import ru.unirate.infrastructure.persistence.repo.ActivityRepository;
import ru.unirate.infrastructure.persistence.repo.AwardFeedItemRepository;
import ru.unirate.infrastructure.persistence.repo.BudgetSpendRepository;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ActivityService {

    private static final Set<Integer> LEVELS = Set.of(25, 50, 75, 100);

    private final ActivityRepository activityRepository;
    private final ActivityEnrollmentRepository enrollmentRepository;
    private final ActivityAwardRepository awardRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentGroupRepository groupRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final PointLedgerRepository ledgerRepository;
    private final AwardFeedItemRepository awardFeedItemRepository;
    private final BudgetSpendRepository budgetSpendRepository;
    private final BudgetStore budgetStore;
    private final ProfileService profileService;
    private final StudentAccessService studentAccessService;
    private final EventService eventService;
    private final SettingsService settingsService;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityEnrollmentRepository enrollmentRepository,
                           ActivityAwardRepository awardRepository,
                           StudentProfileRepository studentProfileRepository,
                           StudentGroupRepository groupRepository,
                           TeacherProfileRepository teacherProfileRepository,
                           PointLedgerRepository ledgerRepository,
                           AwardFeedItemRepository awardFeedItemRepository,
                           BudgetSpendRepository budgetSpendRepository,
                           BudgetStore budgetStore,
                           ProfileService profileService,
                           StudentAccessService studentAccessService,
                           EventService eventService,
                           SettingsService settingsService) {
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.awardRepository = awardRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.groupRepository = groupRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.ledgerRepository = ledgerRepository;
        this.awardFeedItemRepository = awardFeedItemRepository;
        this.budgetSpendRepository = budgetSpendRepository;
        this.budgetStore = budgetStore;
        this.profileService = profileService;
        this.studentAccessService = studentAccessService;
        this.eventService = eventService;
        this.settingsService = settingsService;
    }

    @Transactional
    public ActivityDtos.ActivitySummaryDto create(AuthUser user, ActivityDtos.CreateActivityRequest request) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        List<StudentGroup> groups = resolveGroups(request.groupIds());
        Activity activity = new Activity();
        activity.setTitle(request.title().trim());
        activity.setDescription(request.description() == null ? "" : request.description().trim());
        activity.setType(request.type());
        activity.setEventDate(request.eventDate());
        activity.setMaxPoints(request.maxPoints());
        activity.setCreatedBy(teacher);
        activity.setGroups(new LinkedHashSet<>(groups));
        activityRepository.save(activity);

        teacher.getGroups().addAll(groups);
        teacherProfileRepository.save(teacher);

        for (StudentGroup group : groups) {
            List<StudentProfile> students = studentProfileRepository
                    .findAllByGroupIdAndMembershipStatus(group.getId(), MembershipStatus.CONFIRMED);
            for (StudentProfile student : students) {
                String message = "Новая активность: «" + activity.getTitle() + "»";
                eventService.feedAndNotify(student.getUser(), FeedEventType.ACTIVITY_CREATED, message,
                        "Новая активность", message + ". Максимум: " + activity.getMaxPoints() + " баллов.",
                        "/activities");
            }
        }
        return toSummary(activity, null);
    }

    @Transactional(readOnly = true)
    public List<ActivityDtos.ActivitySummaryDto> list(AuthUser user) {
        if (user.role() == UserRole.STUDENT) {
            StudentProfile profile = studentAccessService.requireStudent(user);
            return activityRepository.findAllByGroupsIdOrderByEventDateDesc(profile.getGroup().getId()).stream()
                    .map(activity -> toSummary(activity, profile))
                    .toList();
        }
        if (user.role() == UserRole.ADMIN) {
            return activityRepository.findAllByOrderByEventDateDesc().stream()
                    .map(activity -> toSummary(activity, null))
                    .toList();
        }
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        return activityRepository.findByCreatedByIdOrderByEventDateDesc(teacher.getId()).stream()
                .map(activity -> toSummary(activity, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityDtos.ActivitySummaryDto detail(AuthUser user, Long activityId) {
        Activity activity = requireActivity(activityId);
        if (user.role() == UserRole.STUDENT) {
            StudentProfile profile = studentAccessService.requireStudent(user);
            requireTargeted(activity, profile);
            return toSummary(activity, profile);
        }
        if (user.role() == UserRole.TEACHER) {
            TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
            requireOwner(activity, teacher);
        }
        return toSummary(activity, null);
    }

    @Transactional(readOnly = true)
    public List<ActivityDtos.ParticipantDto> participants(AuthUser user, Long activityId) {
        Activity activity = requireActivity(activityId);
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        requireOwner(activity, teacher);

        Map<Long, StudentProfile> profiles = new LinkedHashMap<>();
        for (StudentGroup group : activity.getGroups()) {
            studentProfileRepository.findAllByGroupIdAndMembershipStatus(group.getId(), MembershipStatus.CONFIRMED)
                    .forEach(profile -> profiles.putIfAbsent(profile.getId(), profile));
        }
        List<ActivityDtos.ParticipantDto> result = new ArrayList<>();
        for (StudentProfile profile : profiles.values()) {
            boolean enrolled = enrollmentRepository
                    .existsByActivityIdAndStudentProfileId(activity.getId(), profile.getId());
            ActivityAward award = awardRepository
                    .findByActivityIdAndStudentProfileId(activity.getId(), profile.getId()).orElse(null);
            result.add(new ActivityDtos.ParticipantDto(
                    profile.getUser().getId(),
                    profile.getId(),
                    profile.getUser().getFullName(),
                    profile.getGroup().getName(),
                    enrolled,
                    award == null ? null : award.getPoints(),
                    award == null ? null : award.getLevelPercent()
            ));
        }
        result.sort(Comparator.comparing(ActivityDtos.ParticipantDto::fullName));
        return result;
    }

    @Transactional
    public ActivityDtos.ActivitySummaryDto enroll(AuthUser user, Long activityId) {
        StudentProfile student = studentAccessService.requireActiveStudent(user);
        Activity activity = requireActivity(activityId);
        if (activity.getType() != ActivityType.SOCIAL) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                    "Запись доступна только для общественных активностей");
        }
        requireTargeted(activity, student);
        if (activity.getEventDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                    "Запись на прошедшую активность закрыта");
        }
        if (enrollmentRepository.existsByActivityIdAndStudentProfileId(activity.getId(), student.getId())) {
            throw ApiException.conflict(ErrorCodes.ALREADY_ENROLLED, "Вы уже записаны на эту активность");
        }
        ActivityEnrollment enrollment = new ActivityEnrollment();
        enrollment.setActivity(activity);
        enrollment.setStudentProfile(student);
        enrollmentRepository.save(enrollment);
        return toSummary(activity, student);
    }

    @Transactional
    public ActivityDtos.AwardResponse award(AuthUser user, Long activityId, ActivityDtos.AwardRequest request) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        Activity activity = requireActivity(activityId);
        requireOwner(activity, teacher);
        int budgetLimit = settingsService.getInt(SettingsCatalog.TEACHER_MONTHLY_BUDGET_PER_GROUP);
        String yearMonth = YearMonth.now().toString();
        PointCurrency currency = activity.getType() == ActivityType.ACADEMIC ? PointCurrency.BUA : PointCurrency.BOA;

        List<ValidatedAward> validated = new ArrayList<>();
        Set<Long> seenStudents = new HashSet<>();
        for (ActivityDtos.AwardItem item : request.awards()) {
            if (!LEVELS.contains(item.level())) {
                throw ApiException.badRequest(ErrorCodes.INVALID_LEVEL,
                        "Уровень участия должен быть 25, 50, 75 или 100 %");
            }
            StudentProfile student = studentProfileRepository.findByUserId(item.studentId())
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND,
                            "Студент не найден: " + item.studentId()));
            if (!seenStudents.add(student.getId())) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                        "Студент указан в начислении дважды");
            }
            if (!student.isConfirmed()) {
                throw ApiException.forbidden(ErrorCodes.GROUP_NOT_CONFIRMED,
                        "Принадлежность студента " + student.getUser().getFullName() + " к группе не подтверждена");
            }
            requireTargeted(activity, student);
            if (activity.getType() == ActivityType.SOCIAL
                    && !enrollmentRepository.existsByActivityIdAndStudentProfileId(activity.getId(), student.getId())) {
                throw ApiException.badRequest(ErrorCodes.NOT_ENROLLED,
                        "Студент " + student.getUser().getFullName()
                                + " не записан на общественную активность");
            }
            if (awardRepository.existsByActivityIdAndStudentProfileId(activity.getId(), student.getId())) {
                throw ApiException.conflict(ErrorCodes.AWARD_ALREADY_EXISTS,
                        "Баллы за эту активность студенту уже начислены");
            }
            int points = BudgetPolicy.participationPoints(activity.getMaxPoints(), item.level());
            validated.add(new ValidatedAward(student, item.level(), points));
        }

        List<ActivityDtos.AwardResultDto> results = new ArrayList<>();
        for (ValidatedAward item : validated) {
            StudentProfile student = item.student();
            int remaining = budgetStore.spendOrThrow(
                    teacher.getId(),
                    student.getGroup().getId(),
                    yearMonth,
                    item.points(),
                    budgetLimit,
                    student.getGroup().getName()
            );

            ActivityAward award = new ActivityAward();
            award.setActivity(activity);
            award.setStudentProfile(student);
            award.setLevelPercent(item.level());
            award.setPoints(item.points());
            award.setAwardedBy(teacher);
            awardRepository.save(award);

            if (item.points() > 0) {
                PointLedgerEntry entry = new PointLedgerEntry();
                entry.setStudentProfile(student);
                entry.setCurrency(currency);
                entry.setAmount(item.points());
                entry.setReason(activity.getTitle());
                entry.setRefType("ACTIVITY");
                entry.setRefId(activity.getId());
                entry.setActor(teacher.getUser());
                ledgerRepository.save(entry);

                AwardFeedItem feedItem = new AwardFeedItem();
                feedItem.setStudentProfile(student);
                feedItem.setCurrency(currency);
                feedItem.setAmount(item.points());
                feedItem.setActivityTitle(activity.getTitle());
                feedItem.setTeacherName(teacher.getUser().getFullName());
                feedItem.setAward(award);
                awardFeedItemRepository.save(feedItem);

                String message = "Вам начислено +" + item.points() + " "
                        + (currency == PointCurrency.BUA ? "БУА" : "БОА")
                        + " за «" + activity.getTitle() + "»";
                eventService.feedAndNotify(student.getUser(), FeedEventType.POINTS_AWARDED, message,
                        "Начислены баллы активности", message, "/profile");
            }

            results.add(new ActivityDtos.AwardResultDto(
                    student.getUser().getId(),
                    student.getUser().getFullName(),
                    item.points(),
                    currency,
                    remaining
            ));
        }
        return new ActivityDtos.AwardResponse(yearMonth, budgetLimit, results);
    }

    @Transactional(readOnly = true)
    public ActivityDtos.BudgetDto budget(AuthUser user, Long groupId, String yearMonthRaw) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND, "Группа не найдена"));
        if (!teacher.worksWith(group.getId())) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "Вы не работаете с этой группой");
        }
        String yearMonth = normalizeYearMonth(yearMonthRaw);
        int limit = settingsService.getInt(SettingsCatalog.TEACHER_MONTHLY_BUDGET_PER_GROUP);
        int spent = budgetSpendRepository
                .findByTeacherIdAndGroupIdAndYearMonth(teacher.getId(), group.getId(), yearMonth)
                .map(spend -> spend.getAmount())
                .orElse(0);
        return new ActivityDtos.BudgetDto(
                yearMonth,
                group.getId(),
                group.getName(),
                limit,
                spent,
                Math.max(0, limit - spent),
                budgetStore.recentAwards(teacher.getId(), group.getId(), yearMonth)
        );
    }

    @Transactional(readOnly = true)
    public ActivityDtos.BudgetOverviewDto budgets(AuthUser user, String yearMonthRaw) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        String yearMonth = normalizeYearMonth(yearMonthRaw);
        int limit = settingsService.getInt(SettingsCatalog.TEACHER_MONTHLY_BUDGET_PER_GROUP);
        List<ActivityDtos.GroupBudgetDto> groups = teacher.getGroups().stream()
                .sorted(Comparator.comparing(StudentGroup::getName))
                .map(group -> {
                    int spent = budgetSpendRepository
                            .findByTeacherIdAndGroupIdAndYearMonth(teacher.getId(), group.getId(), yearMonth)
                            .map(spend -> spend.getAmount())
                            .orElse(0);
                    return new ActivityDtos.GroupBudgetDto(
                            group.getId(), group.getName(), limit, spent, Math.max(0, limit - spent));
                })
                .toList();
        int totalLimit = limit * groups.size();
        int totalSpent = groups.stream().mapToInt(ActivityDtos.GroupBudgetDto::spent).sum();
        return new ActivityDtos.BudgetOverviewDto(
                yearMonth, totalLimit, totalSpent, Math.max(0, totalLimit - totalSpent), groups);
    }

    private ActivityDtos.ActivitySummaryDto toSummary(Activity activity, StudentProfile student) {
        Boolean enrolled = null;
        Integer myPoints = null;
        Integer myLevel = null;
        if (student != null) {
            enrolled = enrollmentRepository.existsByActivityIdAndStudentProfileId(
                    activity.getId(), student.getId());
            ActivityAward award = awardRepository
                    .findByActivityIdAndStudentProfileId(activity.getId(), student.getId()).orElse(null);
            myPoints = award == null ? null : award.getPoints();
            myLevel = award == null ? null : award.getLevelPercent();
        }
        return new ActivityDtos.ActivitySummaryDto(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getType(),
                activity.getEventDate(),
                activity.getMaxPoints(),
                activity.getGroups().stream()
                        .map(group -> new CatalogDtos.GroupRef(group.getId(), group.getName()))
                        .toList(),
                activity.getCreatedAt(),
                activity.getCreatedBy().getUser().getFullName(),
                enrollmentRepository.countByActivityId(activity.getId()),
                awardRepository.countByActivityId(activity.getId()),
                enrolled,
                myPoints,
                myLevel
        );
    }

    private List<StudentGroup> resolveGroups(List<Long> groupIds) {
        List<StudentGroup> groups = new ArrayList<>();
        for (Long groupId : new LinkedHashSet<>(groupIds)) {
            groups.add(groupRepository.findById(groupId)
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND,
                            "Группа не найдена: " + groupId)));
        }
        return groups;
    }

    private Activity requireActivity(Long activityId) {
        return activityRepository.findWithDetailsById(activityId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ACTIVITY_NOT_FOUND,
                        "Активность не найдена"));
    }

    private void requireOwner(Activity activity, TeacherProfile teacher) {
        if (!activity.getCreatedBy().getId().equals(teacher.getId())) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                    "Эта активность создана другим преподавателем");
        }
    }

    private void requireTargeted(Activity activity, StudentProfile student) {
        boolean targeted = activity.getGroups().stream()
                .anyMatch(group -> group.getId().equals(student.getGroup().getId()));
        if (!targeted) {
            throw ApiException.forbidden(ErrorCodes.GROUP_NOT_TARGETED,
                    "Активность не предназначена для вашей группы");
        }
    }

    private String normalizeYearMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            return YearMonth.now().toString();
        }
        try {
            return YearMonth.parse(raw).toString();
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                    "Месяц должен быть в формате YYYY-MM");
        }
    }

    private record ValidatedAward(StudentProfile student, int level, int points) {
    }
}
