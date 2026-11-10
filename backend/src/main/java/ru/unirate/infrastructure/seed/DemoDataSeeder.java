package ru.unirate.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.ActivityType;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.model.PollQuestionType;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.infrastructure.persistence.entity.Activity;
import ru.unirate.infrastructure.persistence.entity.ActivityAward;
import ru.unirate.infrastructure.persistence.entity.ActivityEnrollment;
import ru.unirate.infrastructure.persistence.entity.AwardFeedItem;
import ru.unirate.infrastructure.persistence.entity.BudgetSpend;
import ru.unirate.infrastructure.persistence.entity.Checkpoint;
import ru.unirate.infrastructure.persistence.entity.FeedEvent;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.infrastructure.persistence.entity.Poll;
import ru.unirate.infrastructure.persistence.entity.PollOption;
import ru.unirate.infrastructure.persistence.entity.PollQuestion;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.repo.ActivityAwardRepository;
import ru.unirate.infrastructure.persistence.repo.ActivityEnrollmentRepository;
import ru.unirate.infrastructure.persistence.repo.ActivityRepository;
import ru.unirate.infrastructure.persistence.repo.AwardFeedItemRepository;
import ru.unirate.infrastructure.persistence.repo.BudgetSpendRepository;
import ru.unirate.infrastructure.persistence.repo.CheckpointRepository;
import ru.unirate.infrastructure.persistence.repo.FeedEventRepository;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.PollRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Enriches the V2 reference seed with demo content dated relative to startup so the
 * defence demo always has an active task, an overdue task, activities, awards and polls.
 * Idempotent: existing demo titles are not duplicated.
 */
@Component
@Profile("!worker")
@ConditionalOnProperty(name = "unirate.demo-seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final CheckpointRepository checkpointRepository;
    private final StudentGroupRepository groupRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubmissionRepository submissionRepository;
    private final ActivityRepository activityRepository;
    private final ActivityEnrollmentRepository enrollmentRepository;
    private final ActivityAwardRepository awardRepository;
    private final AwardFeedItemRepository awardFeedRepository;
    private final PointLedgerRepository ledgerRepository;
    private final BudgetSpendRepository budgetSpendRepository;
    private final PollRepository pollRepository;
    private final FeedEventRepository feedEventRepository;

    public DemoDataSeeder(CheckpointRepository checkpointRepository,
                          StudentGroupRepository groupRepository,
                          StudentProfileRepository studentProfileRepository,
                          TeacherProfileRepository teacherProfileRepository,
                          SubmissionRepository submissionRepository,
                          ActivityRepository activityRepository,
                          ActivityEnrollmentRepository enrollmentRepository,
                          ActivityAwardRepository awardRepository,
                          AwardFeedItemRepository awardFeedRepository,
                          PointLedgerRepository ledgerRepository,
                          BudgetSpendRepository budgetSpendRepository,
                          PollRepository pollRepository,
                          FeedEventRepository feedEventRepository) {
        this.checkpointRepository = checkpointRepository;
        this.groupRepository = groupRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.submissionRepository = submissionRepository;
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.awardRepository = awardRepository;
        this.awardFeedRepository = awardFeedRepository;
        this.ledgerRepository = ledgerRepository;
        this.budgetSpendRepository = budgetSpendRepository;
        this.pollRepository = pollRepository;
        this.feedEventRepository = feedEventRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        TeacherProfile teacher = teacherProfileRepository.findById(1L).orElse(null);
        StudentGroup group1 = groupRepository.findById(1L).orElse(null);
        StudentGroup group2 = groupRepository.findById(2L).orElse(null);
        if (teacher == null || group1 == null) {
            log.info("Demo seed skipped: reference data is missing");
            return;
        }
        StudentProfile student = studentProfileRepository.findByUserId(4L).orElse(null);
        StudentProfile lead = studentProfileRepository.findByUserId(6L).orElse(null);
        StudentProfile group2Student = studentProfileRepository.findByUserId(7L).orElse(null);

        Checkpoint active = ensureCheckpoint(teacher, group1,
                "Курсовая работа, 1 этап",
                "Подготовьте первый раздел курсовой работы: введение, обзор литературы и постановку задачи.",
                Instant.now().plus(5, ChronoUnit.DAYS), 100);
        Checkpoint overdue = ensureCheckpoint(teacher, group1,
                "Лабораторная работа 3",
                "Реализуйте алгоритм обработки данных и приложите отчёт с результатами тестирования.",
                Instant.now().minus(1, ChronoUnit.DAYS), 60);
        Checkpoint group2Checkpoint = null;
        if (group2 != null) {
            group2Checkpoint = ensureCheckpoint(teacher, group2,
                    "Курсовая работа, 2 этап",
                    "Доработайте реализацию и подготовьте презентацию результатов.",
                    Instant.now().plus(10, ChronoUnit.DAYS), 100);
        }
        assign(active, group1);
        assign(overdue, group1);
        assign(group2Checkpoint, group2);

        Activity pastSocial = ensureActivity(teacher, group1,
                "Помощь в организации конференции",
                "Студенты помогали с регистрацией участников и организацией секций.",
                ActivityType.SOCIAL, LocalDate.now().minusDays(10), 100);
        Activity upcomingAcademic = ensureActivity(teacher, group1,
                "Научный семинар «Алгоритмы»",
                "Семинар кафедры программирования: доклады студентов и разбор задач.",
                ActivityType.ACADEMIC, LocalDate.now().plusDays(3), 100);
        Activity upcomingSocial = ensureActivity(teacher, group1,
                "Волонтёрство на Дне открытых дверей",
                "Помощь в проведении дня открытых дверей университета.",
                ActivityType.SOCIAL, LocalDate.now().plusDays(5), 100);
        ensureActivity(teacher, group2 != null ? group2 : group1,
                "Хакатон UniRate",
                "Командная разработка прототипа университетского сервиса.",
                ActivityType.ACADEMIC, LocalDate.now().plusDays(7), 150);

        if (student != null) {
            enroll(upcomingSocial, student);
            seedAward(pastSocial, student, 50, teacher);
        }
        if (lead != null) {
            enroll(upcomingSocial, lead);
            seedAward(pastSocial, lead, 25, teacher);
        }
        if (group2Student != null) {
            Activity pastAcademic = ensureActivity(teacher, group2,
                    "Доклад на научном семинаре",
                    "Студент подготовил и представил доклад по теме исследования.",
                    ActivityType.ACADEMIC, LocalDate.now().minusDays(7), 100);
            seedAward(pastAcademic, group2Student, 50, teacher);
        }

        ensurePoll(teacher, group1, "Формат итогового зачёта",
                "Выберите предпочтительный формат итогового зачёта по курсу.",
                Instant.now().plus(7, ChronoUnit.DAYS), true,
                List.of(pollQuestion("Предпочтительный формат",
                        PollQuestionType.SINGLE, List.of("Устно", "Письменно", "Проект"))));
        ensurePoll(teacher, group1, "Выбор темы курсовой работы",
                "Отметьте темы, которые вам интересны. Можно выбрать несколько вариантов.",
                Instant.now().plus(10, ChronoUnit.DAYS), false,
                List.of(pollQuestion("Темы курсовых",
                        PollQuestionType.MULTIPLE,
                        List.of("Веб-приложения", "Машинное обучение", "Базы данных", "Компьютерные сети"))));

        log.info("Demo seed ensured: checkpoints, activities, awards and polls");
    }

    private Checkpoint ensureCheckpoint(TeacherProfile teacher, StudentGroup group, String title,
                                        String description, Instant deadline, int cost) {
        if (checkpointRepository.existsByTitle(title)) {
            return null;
        }
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setTitle(title);
        checkpoint.setDescription(description);
        checkpoint.setDeadline(deadline);
        checkpoint.setCost(cost);
        checkpoint.setCreatedBy(teacher);
        checkpoint.setGroups(new LinkedHashSet<>(List.of(group)));
        return checkpointRepository.save(checkpoint);
    }

    private void assign(Checkpoint checkpoint, StudentGroup group) {
        if (checkpoint == null) {
            return;
        }
        List<StudentProfile> students = studentProfileRepository
                .findAllByGroupIdAndMembershipStatus(group.getId(), MembershipStatus.CONFIRMED);
        for (StudentProfile profile : students) {
            if (submissionRepository.findByCheckpointIdAndStudentProfileId(
                    checkpoint.getId(), profile.getId()).isPresent()) {
                continue;
            }
            Submission submission = new Submission();
            submission.setCheckpoint(checkpoint);
            submission.setStudentProfile(profile);
            submission.setStatus(SubmissionStatus.IN_PROGRESS);
            submissionRepository.save(submission);

            FeedEvent event = new FeedEvent();
            event.setUser(profile.getUser());
            event.setEventType(FeedEventType.CHECKPOINT_ASSIGNED.name());
            event.setMessage("Преподаватель назначил задачу «" + checkpoint.getTitle() + "» вашей группе");
            feedEventRepository.save(event);
        }
    }

    private Activity ensureActivity(TeacherProfile teacher, StudentGroup group, String title,
                                    String description, ActivityType type, LocalDate date, int maxPoints) {
        if (activityRepository.existsByTitle(title)) {
            return null;
        }
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setType(type);
        activity.setEventDate(date);
        activity.setMaxPoints(maxPoints);
        activity.setCreatedBy(teacher);
        activity.setGroups(new LinkedHashSet<>(List.of(group)));
        return activityRepository.save(activity);
    }

    private void enroll(Activity activity, StudentProfile student) {
        if (activity == null || enrollmentRepository.existsByActivityIdAndStudentProfileId(
                activity.getId(), student.getId())) {
            return;
        }
        ActivityEnrollment enrollment = new ActivityEnrollment();
        enrollment.setActivity(activity);
        enrollment.setStudentProfile(student);
        enrollmentRepository.save(enrollment);
    }

    private void seedAward(Activity activity, StudentProfile student, int level, TeacherProfile teacher) {
        if (activity == null || awardRepository.existsByActivityIdAndStudentProfileId(
                activity.getId(), student.getId())) {
            return;
        }
        int points = Math.floorDiv(activity.getMaxPoints() * level, 100);
        PointCurrency currency = activity.getType() == ActivityType.ACADEMIC
                ? PointCurrency.BUA : PointCurrency.BOA;

        ActivityAward award = new ActivityAward();
        award.setActivity(activity);
        award.setStudentProfile(student);
        award.setLevelPercent(level);
        award.setPoints(points);
        award.setAwardedBy(teacher);
        awardRepository.save(award);

        PointLedgerEntry entry = new PointLedgerEntry();
        entry.setStudentProfile(student);
        entry.setCurrency(currency);
        entry.setAmount(points);
        entry.setReason(activity.getTitle());
        entry.setRefType("ACTIVITY");
        entry.setRefId(activity.getId());
        entry.setActor(teacher.getUser());
        ledgerRepository.save(entry);

        AwardFeedItem feedItem = new AwardFeedItem();
        feedItem.setStudentProfile(student);
        feedItem.setCurrency(currency);
        feedItem.setAmount(points);
        feedItem.setActivityTitle(activity.getTitle());
        feedItem.setTeacherName(teacher.getUser().getFullName());
        feedItem.setAward(award);
        awardFeedRepository.save(feedItem);

        String yearMonth = YearMonth.now().toString();
        BudgetSpend spend = budgetSpendRepository
                .findByTeacherIdAndGroupIdAndYearMonth(teacher.getId(), student.getGroup().getId(), yearMonth)
                .orElseGet(() -> {
                    BudgetSpend created = new BudgetSpend();
                    created.setTeacher(teacher);
                    created.setGroup(student.getGroup());
                    created.setYearMonth(yearMonth);
                    created.setAmount(0);
                    return created;
                });
        spend.setAmount(spend.getAmount() + points);
        budgetSpendRepository.save(spend);
    }

    private Poll ensurePoll(TeacherProfile teacher, StudentGroup group, String title, String description,
                            Instant endsAt, boolean anonymous, List<PollQuestion> questions) {
        if (pollRepository.existsByTitle(title)) {
            return null;
        }
        Poll poll = new Poll();
        poll.setTitle(title);
        poll.setDescription(description);
        poll.setEndsAt(endsAt);
        poll.setAnonymous(anonymous);
        poll.setCreatedBy(teacher);
        poll.setGroups(new LinkedHashSet<>(List.of(group)));
        for (PollQuestion question : questions) {
            question.setPoll(poll);
            poll.getQuestions().add(question);
        }
        return pollRepository.save(poll);
    }

    private PollQuestion pollQuestion(String text, PollQuestionType type, List<String> options) {
        PollQuestion question = new PollQuestion();
        question.setText(text);
        question.setType(type);
        int order = 0;
        for (String optionText : options) {
            PollOption option = new PollOption();
            option.setQuestion(question);
            option.setText(optionText);
            option.setSortOrder(order++);
            question.getOptions().add(option);
        }
        return question;
    }
}
