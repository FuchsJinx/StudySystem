package ru.unirate.infrastructure.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Skeleton: ticks every minute; real debit logic lands in RatingService (task B4).
 */
@Component
@ConditionalOnProperty(name = "unirate.worker.enabled", havingValue = "true")
public class ArDeductionJob {

    private static final Logger log = LoggerFactory.getLogger(ArDeductionJob.class);

    @Scheduled(fixedDelayString = "${unirate.worker.deduct-delay-ms:60000}")
    public void tick() {
        log.info("ArDeductionJob tick (stub — wire RatingService in B4)");
    }
}
