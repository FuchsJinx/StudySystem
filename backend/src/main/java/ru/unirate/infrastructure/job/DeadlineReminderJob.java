package ru.unirate.infrastructure.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.unirate.application.DeadlineReminderProcessor;

@Component
@ConditionalOnProperty(name = "unirate.worker.enabled", havingValue = "true")
public class DeadlineReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderJob.class);

    private final DeadlineReminderProcessor processor;

    public DeadlineReminderJob(DeadlineReminderProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${unirate.worker.reminder-delay-ms:300000}")
    public void tick() {
        try {
            int created = processor.process();
            if (created > 0) {
                log.info("DeadlineReminderJob: created {} reminders", created);
            }
        } catch (RuntimeException ex) {
            log.error("DeadlineReminderJob failed", ex);
        }
    }
}
