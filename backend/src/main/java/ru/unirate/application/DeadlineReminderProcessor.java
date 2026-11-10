package ru.unirate.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.DeadlineReminder;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.repo.DeadlineReminderRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/** Creates one DEADLINE_SOON feed item/notification per (student, checkpoint) (spec 12 / PF-02). */
@Service
public class DeadlineReminderProcessor {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderProcessor.class);

    private final SubmissionRepository submissionRepository;
    private final DeadlineReminderRepository reminderRepository;
    private final SettingsService settingsService;
    private final EventService eventService;

    public DeadlineReminderProcessor(SubmissionRepository submissionRepository,
                                     DeadlineReminderRepository reminderRepository,
                                     SettingsService settingsService,
                                     EventService eventService) {
        this.submissionRepository = submissionRepository;
        this.reminderRepository = reminderRepository;
        this.settingsService = settingsService;
        this.eventService = eventService;
    }

    @Transactional
    public int process() {
        int hours = settingsService.getInt(SettingsCatalog.DEADLINE_REMINDER_HOURS);
        Instant now = Instant.now();
        Instant threshold = now.plus(hours, ChronoUnit.HOURS);
        List<Submission> soon = submissionRepository.findDeadlineSoon(
                now, threshold, Set.of(SubmissionStatus.IN_PROGRESS, SubmissionStatus.RETURNED));
        int created = 0;
        for (Submission submission : soon) {
            Long studentProfileId = submission.getStudentProfile().getId();
            Long checkpointId = submission.getCheckpoint().getId();
            if (reminderRepository.existsByStudentProfileIdAndCheckpointId(studentProfileId, checkpointId)) {
                continue;
            }
            DeadlineReminder reminder = new DeadlineReminder();
            reminder.setStudentProfile(submission.getStudentProfile());
            reminder.setCheckpoint(submission.getCheckpoint());
            try {
                reminderRepository.saveAndFlush(reminder);
            } catch (DataIntegrityViolationException ex) {
                log.debug("Deadline reminder already exists for student={}, checkpoint={}",
                        studentProfileId, checkpointId);
                continue;
            }
            String message = "До дедлайна «" + submission.getCheckpoint().getTitle()
                    + "» осталось менее " + hours + " ч";
            eventService.feedAndNotify(submission.getStudentProfile().getUser(),
                    FeedEventType.DEADLINE_SOON, message, "Скоро дедлайн",
                    message + ". Дедлайн: " + DateFormats.deadline(submission.getCheckpoint().getDeadline())
                            + ". Сдайте работу, чтобы остановить списание АР.",
                    "/tasks");
            created++;
        }
        return created;
    }
}
