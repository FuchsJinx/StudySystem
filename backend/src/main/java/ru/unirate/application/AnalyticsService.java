package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.ActivityDtos;
import ru.unirate.api.dto.AnalyticsDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.BudgetSpend;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.repo.ActivityRepository;
import ru.unirate.infrastructure.persistence.repo.BudgetSpendRepository;
import ru.unirate.infrastructure.persistence.repo.CheckpointRepository;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.PollRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private final StudentProfileRepository studentProfileRepository;
    private final StudentGroupRepository groupRepository;
    private final PointLedgerRepository ledgerRepository;
    private final CheckpointRepository checkpointRepository;
    private final ActivityRepository activityRepository;
    private final PollRepository pollRepository;
    private final BudgetSpendRepository budgetSpendRepository;
    private final ProfileService profileService;
    private final SettingsService settingsService;

    public AnalyticsService(StudentProfileRepository studentProfileRepository,
                            StudentGroupRepository groupRepository,
                            PointLedgerRepository ledgerRepository,
                            CheckpointRepository checkpointRepository,
                            ActivityRepository activityRepository,
                            PollRepository pollRepository,
                            BudgetSpendRepository budgetSpendRepository,
                            ProfileService profileService,
                            SettingsService settingsService) {
        this.studentProfileRepository = studentProfileRepository;
        this.groupRepository = groupRepository;
        this.ledgerRepository = ledgerRepository;
        this.checkpointRepository = checkpointRepository;
        this.activityRepository = activityRepository;
        this.pollRepository = pollRepository;
        this.budgetSpendRepository = budgetSpendRepository;
        this.profileService = profileService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.ArDynamicsResponse arDynamics(AuthUser user, Long groupId, int days) {
        StudentGroup group = requireGroupAccess(user, groupId);
        int window = Math.max(7, Math.min(days, 90));
        LocalDate endDate = LocalDate.now(ZONE);
        LocalDate startDate = endDate.minusDays(window - 1L);
        Instant from = startDate.atStartOfDay(ZONE).toInstant();

        List<StudentProfile> students = studentProfileRepository
                .findAllByGroupIdAndMembershipStatus(groupId, MembershipStatus.CONFIRMED);
        Map<Long, Integer> currentRatings = students.stream()
                .collect(Collectors.toMap(StudentProfile::getId, StudentProfile::getAcademicRating));
        Map<Long, List<PointLedgerEntry>> entriesByStudent = new HashMap<>();
        for (PointLedgerEntry entry : ledgerRepository.findForGroupSince(groupId, PointCurrency.AR, from)) {
            entriesByStudent.computeIfAbsent(entry.getStudentProfile().getId(), key -> new ArrayList<>())
                    .add(entry);
        }

        List<AnalyticsDtos.ArPointDto> points = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Instant endExclusive = date.plusDays(1).atStartOfDay(ZONE).toInstant();
            List<Integer> ratings = new ArrayList<>(students.size());
            for (StudentProfile student : students) {
                int laterChanges = entriesByStudent.getOrDefault(student.getId(), List.of()).stream()
                        .filter(entry -> !entry.getCreatedAt().isBefore(endExclusive))
                        .mapToInt(PointLedgerEntry::getAmount)
                        .sum();
                ratings.add(currentRatings.getOrDefault(student.getId(), 0) - laterChanges);
            }
            points.add(new AnalyticsDtos.ArPointDto(date, average(ratings), median(ratings)));
        }
        List<AnalyticsDtos.StudentArDto> studentDtos = students.stream()
                .sorted(Comparator.comparing(student -> student.getUser().getFullName()))
                .map(student -> new AnalyticsDtos.StudentArDto(
                        student.getUser().getId(), student.getUser().getFullName(),
                        student.getAcademicRating()))
                .toList();
        return new AnalyticsDtos.ArDynamicsResponse(group.getId(), group.getName(), points, studentDtos);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.ActivityRankingResponse activityRanking(AuthUser user, Long groupId) {
        StudentGroup group = requireGroupAccess(user, groupId);
        List<StudentProfile> students = studentProfileRepository
                .findAllByGroupIdAndMembershipStatus(groupId, MembershipStatus.CONFIRMED);
        List<AnalyticsDtos.RankRowDto> rows = new ArrayList<>();
        for (StudentProfile student : students) {
            long bua = ledgerRepository.sumCredits(student.getId(), PointCurrency.BUA);
            long boa = ledgerRepository.sumCredits(student.getId(), PointCurrency.BOA);
            rows.add(new AnalyticsDtos.RankRowDto(
                    student.getUser().getId(),
                    student.getUser().getFullName(),
                    group.getName(),
                    bua,
                    boa,
                    bua + boa
            ));
        }
        rows.sort(Comparator.comparingLong(AnalyticsDtos.RankRowDto::bua).reversed()
                .thenComparing(Comparator.comparingLong(AnalyticsDtos.RankRowDto::boa).reversed()));
        return new AnalyticsDtos.ActivityRankingResponse(group.getId(), group.getName(), rows);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.BudgetStatsResponse budgetStats(AuthUser user, int months) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        int window = Math.max(1, Math.min(months, 12));
        int limit = settingsService.getInt(SettingsCatalog.TEACHER_MONTHLY_BUDGET_PER_GROUP);
        YearMonth current = YearMonth.now(ZONE);
        List<StudentGroup> groups = teacher.getGroups().stream()
                .sorted(Comparator.comparing(StudentGroup::getName))
                .toList();
        List<AnalyticsDtos.MonthBudgetDto> result = new ArrayList<>();
        for (int i = 0; i < window; i++) {
            YearMonth month = current.minusMonths(i);
            Map<Long, Integer> spends = budgetSpendRepository
                    .findByTeacherIdAndYearMonth(teacher.getId(), month.toString()).stream()
                    .collect(Collectors.toMap(spend -> spend.getGroup().getId(), BudgetSpend::getAmount));
            int totalSpent = 0;
            List<AnalyticsDtos.GroupSpendDto> groupSpends = new ArrayList<>();
            for (StudentGroup group : groups) {
                int spent = spends.getOrDefault(group.getId(), 0);
                totalSpent += spent;
                groupSpends.add(new AnalyticsDtos.GroupSpendDto(
                        group.getId(), group.getName(), limit, spent));
            }
            result.add(new AnalyticsDtos.MonthBudgetDto(
                    month.toString(), limit * groups.size(), totalSpent, groupSpends));
        }
        return new AnalyticsDtos.BudgetStatsResponse(result);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.TeacherOverviewResponse teacherOverview(AuthUser user) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        int limit = settingsService.getInt(SettingsCatalog.TEACHER_MONTHLY_BUDGET_PER_GROUP);
        String yearMonth = YearMonth.now(ZONE).toString();
        Map<Long, Integer> spends = budgetSpendRepository
                .findByTeacherIdAndYearMonth(teacher.getId(), yearMonth).stream()
                .collect(Collectors.toMap(spend -> spend.getGroup().getId(), BudgetSpend::getAmount));
        List<ActivityDtos.GroupBudgetDto> budget = new ArrayList<>();
        int students = 0;
        int pending = 0;
        for (StudentGroup group : teacher.getGroups().stream()
                .sorted(Comparator.comparing(StudentGroup::getName)).toList()) {
            int groupStudents = (int) studentProfileRepository.countByGroupId(group.getId());
            students += groupStudents;
            pending += (int) studentProfileRepository.countByGroupIdAndMembershipStatus(
                    group.getId(), MembershipStatus.PENDING);
            int spent = spends.getOrDefault(group.getId(), 0);
            budget.add(new ActivityDtos.GroupBudgetDto(
                    group.getId(), group.getName(), limit, spent, Math.max(0, limit - spent)));
        }
        return new AnalyticsDtos.TeacherOverviewResponse(
                teacher.getGroups().size(),
                students,
                (int) checkpointRepository.countByCreatedById(teacher.getId()),
                (int) activityRepository.countByCreatedById(teacher.getId()),
                (int) pollRepository.countByCreatedById(teacher.getId()),
                pending,
                yearMonth,
                budget
        );
    }

    private StudentGroup requireGroupAccess(AuthUser user, Long groupId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND, "Группа не найдена"));
        if (user.isAdmin()) {
            return group;
        }
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        if (!teacher.worksWith(group.getId())) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                    "Вы не работаете с этой группой");
        }
        return group;
    }

    private double average(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0) * 10.0) / 10.0;
    }

    private int median(List<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return (int) Math.round((sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0);
    }
}
