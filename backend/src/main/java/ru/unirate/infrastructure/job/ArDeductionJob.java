package ru.unirate.infrastructure.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.unirate.application.RatingDeductionService;
import ru.unirate.application.SettingsService;
import ru.unirate.domain.settings.SettingsCatalog;

import java.time.Duration;
import java.time.Instant;

/**
 * AR deduction worker. The scheduler wakes every second, but the actual tick runs
 * according to the interval configured in app_settings (spec 03 / AR-18).
 */
@Component
@ConditionalOnProperty(name = "unirate.worker.enabled", havingValue = "true")
public class ArDeductionJob {

    private static final Logger log = LoggerFactory.getLogger(ArDeductionJob.class);

    private final RatingDeductionService deductionService;
    private final SettingsService settingsService;
    private volatile Instant lastRun = Instant.EPOCH;

    public ArDeductionJob(RatingDeductionService deductionService, SettingsService settingsService) {
        this.deductionService = deductionService;
        this.settingsService = settingsService;
    }

    @Scheduled(fixedDelayString = "${unirate.worker.tick-ms:1000}")
    public void tick() {
        Instant now = Instant.now();
        int intervalSeconds = settingsService.getInt(SettingsCatalog.AR_DEDUCT_INTERVAL_SEC);
        if (Duration.between(lastRun, now).getSeconds() < intervalSeconds) {
            return;
        }
        lastRun = now;
        try {
            RatingDeductionService.DeductionResult result = deductionService.tick();
            log.info("ArDeductionJob tick: overdueMarked={}, debitedStudents={}, totalDebited={}",
                    result.markedOverdue(), result.debitedStudents(), result.totalDebited());
        } catch (RuntimeException ex) {
            log.error("ArDeductionJob tick failed", ex);
        }
    }
}
