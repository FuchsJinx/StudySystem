package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.AiDtos;
import ru.unirate.api.dto.PollDtos;
import ru.unirate.api.dto.ProfileDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.ai.PromptTemplates;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.infrastructure.llm.LlmClient;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AiService {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Moscow"));

    private final LlmClient llmClient;
    private final ProfileService profileService;
    private final PollService pollService;
    private final StudentProfileRepository studentProfileRepository;
    private final SubmissionRepository submissionRepository;

    public AiService(LlmClient llmClient,
                     ProfileService profileService,
                     PollService pollService,
                     StudentProfileRepository studentProfileRepository,
                     SubmissionRepository submissionRepository) {
        this.llmClient = llmClient;
        this.profileService = profileService;
        this.pollService = pollService;
        this.studentProfileRepository = studentProfileRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public AiDtos.AiTextResponse draftCheckpoint(AuthUser user, AiDtos.DraftCheckpointRequest request) {
        profileService.requireTeacherProfile(user.id());
        String userPrompt = PromptTemplates.draftCheckpoint(request.title().trim(), request.theses().trim());
        Optional<String> generated = llmClient.generate(PromptTemplates.DRAFT_SYSTEM, userPrompt);
        String text = generated.orElseGet(() -> demoDraft(request.title().trim(), request.theses().trim()));
        return new AiDtos.AiTextResponse(text, llmClient.provider());
    }

    @Transactional(readOnly = true)
    public AiDtos.AiTextResponse pollSummary(AuthUser user, Long pollId) {
        PollDtos.PollResultsDto results = pollService.results(user, pollId);
        String aggregate = aggregateText(results);
        Optional<String> generated = llmClient.generate(
                PromptTemplates.POLL_SUMMARY_SYSTEM, PromptTemplates.pollSummary(aggregate));
        String text = generated.orElseGet(() -> demoPollSummary(results));
        return new AiDtos.AiTextResponse(text, llmClient.provider());
    }

    @Transactional(readOnly = true)
    public AiDtos.RatingTipsResponse ratingTips(AuthUser user) {
        ProfileDtos.UserMe me = profileService.userMe(user.id());
        if (me.student() == null) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "Советы доступны студентам");
        }
        StudentProfile profile = studentProfileRepository.findByUserId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "Профиль студента не найден"));
        List<Submission> submissions = submissionRepository
                .findByStudentProfileIdOrderByCheckpointDeadlineDesc(profile.getId());
        long overdue = submissions.stream()
                .filter(submission -> submission.getStatus() == SubmissionStatus.OVERDUE).count();
        long active = submissions.stream()
                .filter(submission -> Set.of(SubmissionStatus.IN_PROGRESS, SubmissionStatus.SUBMITTED,
                        SubmissionStatus.OVERDUE, SubmissionStatus.RETURNED).contains(submission.getStatus()))
                .count();
        Instant nearest = submissions.stream()
                .filter(submission -> submission.getStatus() != SubmissionStatus.ACCEPTED)
                .map(submission -> submission.getCheckpoint().getDeadline())
                .filter(deadline -> deadline.isAfter(Instant.now()))
                .min(Comparator.naturalOrder())
                .orElse(null);
        String nearestText = nearest == null ? "нет" : DEADLINE_FORMAT.format(nearest);

        String prompt = PromptTemplates.ratingTips(
                me.student().academicRating(), me.student().bua(), me.student().boa(),
                overdue, active, nearestText);
        Optional<String> generated = llmClient.generate(PromptTemplates.RATING_TIPS_SYSTEM, prompt);
        List<String> tips = generated.map(this::parseTips).orElse(List.of());
        if (tips.size() < 3) {
            tips = demoTips(me.student().academicRating(), me.student().bua(), me.student().boa(),
                    (int) overdue, (int) active, nearestText);
        }
        return new AiDtos.RatingTipsResponse(tips, llmClient.provider());
    }

    private List<String> parseTips(String generated) {
        List<String> tips = new ArrayList<>();
        for (String line : generated.split("\\r?\\n")) {
            String cleaned = line.trim()
                    .replaceFirst("^[-*•\\d]+[.)\\s]*", "")
                    .trim();
            if (cleaned.length() > 3) {
                tips.add(cleaned);
            }
            if (tips.size() == 5) {
                break;
            }
        }
        return tips;
    }

    private List<String> demoTips(int academicRating, long bua, long boa, int overdue,
                                  int active, String nearestDeadline) {
        List<String> tips = new ArrayList<>();
        if (overdue > 0) {
            tips.add("Сдайте просроченные работы — это остановит списание академического рейтинга.");
        }
        if (academicRating < 1000) {
            tips.add("Планируйте сдачу за 1–2 дня до дедлайна: рейтинг списывается автоматически "
                    + "по 20 АР за тик, пока работа не сдана.");
        }
        if (nearestDeadline != null && !nearestDeadline.equals("нет")) {
            tips.add("Ближайший дедлайн — " + nearestDeadline
                    + ". Разбейте подготовку на небольшие шаги и начните сегодня.");
        }
        if (bua == 0) {
            tips.add("Запишитесь на учебные активности: они дают баллы БУА и не влияют на списание АР.");
        } else {
            tips.add("Поддерживайте учебную активность: участвуйте в семинарах и конкурсах для роста БУА.");
        }
        if (boa == 0) {
            tips.add("Попробуйте общественную активность — волонтёрство или организацию мероприятий: "
                    + "это баллы БОА и новые навыки.");
        } else {
            tips.add("Продолжайте общественные активности — БОА выделяет вас в ленте наград.");
        }
        if (active == 0) {
            tips.add("Следите за новыми заданиями и опросами в разделе «Мои задачи» и «Опросы».");
        }
        List<String> generic = List.of(
                "Проверяйте дедлайны в персональной ленте и включайте напоминания в профиле.",
                "Участвуйте в опросах преподавателей — это помогает влиять на учебный процесс.",
                "Держите баланс между учебными и общественными активностями: БУА и БОА ценятся отдельно."
        );
        int genericIndex = 0;
        while (tips.size() < 3 && genericIndex < generic.size()) {
            if (!tips.contains(generic.get(genericIndex))) {
                tips.add(generic.get(genericIndex));
            }
            genericIndex++;
        }
        int limit = Math.min(5, tips.size());
        return new ArrayList<>(tips.subList(0, limit));
    }

    private String demoDraft(String title, String theses) {
        String normalizedTheses = theses.replaceAll("\\s+", " ").trim();
        return """
                Цель работы
                Выполнить контрольную точку «%s» и продемонстрировать понимание ключевых понятий темы.

                Что нужно сделать
                %s

                Требования к результату
                Оформите результат в виде отчёта или ссылки на репозиторий. Укажите использованные источники
                и кратко опишите ход решения. Работа должна быть сдана до дедлайна.

                Критерии оценки
                Полнота раскрытия темы, корректность и самостоятельность решения, аккуратность оформления.
                """.formatted(title, normalizedTheses);
    }

    private String demoPollSummary(PollDtos.PollResultsDto results) {
        if (results.totalResponses() == 0) {
            return "Ответов пока нет. Сводка появится после того, как студенты проголосуют.";
        }
        StringBuilder summary = new StringBuilder();
        summary.append("Всего ответов: ").append(results.totalResponses()).append(".\n");
        for (PollDtos.QuestionResultDto question : results.results()) {
            summary.append("\n").append(question.text()).append(":\n");
            question.options().stream()
                    .sorted(Comparator.comparingInt(PollDtos.OptionResultDto::count).reversed())
                    .forEach(option -> summary.append("— ").append(option.text())
                            .append(": ").append(option.count()).append(" (")
                            .append(option.percent()).append("%)\n"));
            question.options().stream()
                    .max(Comparator.comparingInt(PollDtos.OptionResultDto::count))
                    .ifPresent(leader -> summary.append("Лидирует вариант «")
                            .append(leader.text()).append("»."));
        }
        return summary.toString().trim();
    }

    private String aggregateText(PollDtos.PollResultsDto results) {
        StringBuilder text = new StringBuilder();
        text.append("Опрос: ").append(results.title()).append("\n");
        text.append("Всего ответов: ").append(results.totalResponses()).append("\n");
        for (PollDtos.QuestionResultDto question : results.results()) {
            text.append("Вопрос: ").append(question.text()).append(" (").append(question.totalVotes())
                    .append(" ответов)\n");
            for (PollDtos.OptionResultDto option : question.options()) {
                text.append("  - ").append(option.text()).append(": ").append(option.count())
                        .append(" (").append(option.percent()).append("%)\n");
            }
        }
        return text.toString();
    }
}
