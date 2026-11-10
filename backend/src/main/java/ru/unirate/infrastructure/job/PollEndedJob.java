package ru.unirate.infrastructure.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.unirate.application.PollEndedProcessor;

@Component
@ConditionalOnProperty(name = "unirate.worker.enabled", havingValue = "true")
public class PollEndedJob {

    private static final Logger log = LoggerFactory.getLogger(PollEndedJob.class);

    private final PollEndedProcessor processor;

    public PollEndedJob(PollEndedProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${unirate.worker.poll-delay-ms:60000}")
    public void tick() {
        try {
            int processed = processor.processEnded();
            if (processed > 0) {
                log.info("PollEndedJob: processed {} polls", processed);
            }
        } catch (RuntimeException ex) {
            log.error("PollEndedJob failed", ex);
        }
    }
}
