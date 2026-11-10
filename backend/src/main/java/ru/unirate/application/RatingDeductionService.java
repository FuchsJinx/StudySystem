package ru.unirate.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.model.SubmissionStatus;
import ru.unirate.domain.rating.RatingEngine;
import ru.unirate.domain.settings.SettingsCatalog;
import ru.unirate.infrastructure.persistence.entity.PointLedgerEntry;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.Submission;
import ru.unirate.infrastructure.persistence.repo.PointLedgerRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.SubmissionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Executes one AR deduction tick per spec 03: cost cap, AR floor, submit/accept stops deduction. */
@Service
public class RatingDeductionService {

    private static final Logger log = LoggerFactory.getLogger(RatingDeductionService.class);
    private static final Set<SubmissionStatus> DEDUCTIBLE = Set.of(
            SubmissionStatus.IN_PROGRESS,
            SubmissionStatus.OVERDUE,
            SubmissionStatus.RETURNED
    );

    private final SubmissionRepository submissionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PointLedgerRepository ledgerRepository;
    private final SettingsService settingsService;

    public RatingDeductionService(SubmissionRepository submissionRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  PointLedgerRepository ledgerRepository,
                                  SettingsService settingsService) {
        this.submissionRepository = submissionRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.ledgerRepository = ledgerRepository;
        this.settingsService = settingsService;
    }

    @Transactional
    public DeductionResult tick() {
        Instant now = Instant.now();
        int markedOverdue = submissionRepository.markOverdue(
                SubmissionStatus.IN_PROGRESS, SubmissionStatus.OVERDUE, now);
        List<Submission> candidates = submissionRepository.findDebitCandidates(now, DEDUCTIBLE);
        int amount = settingsService.getInt(SettingsCatalog.AR_DEDUCT_AMOUNT);
        int debitedStudents = 0;
        int totalDebited = 0;

        for (Submission candidate : candidates) {
            Submission submission = submissionRepository.findForUpdate(candidate.getId()).orElse(null);
            if (submission == null) {
                continue;
            }
            StudentProfile student = studentProfileRepository
                    .findForUpdate(submission.getStudentProfile().getId()).orElse(null);
            if (student == null) {
                continue;
            }
            // Re-check after acquiring locks: another worker may have submitted/accepted or debited meanwhile.
            if (!submission.getCheckpoint().getDeadline().isBefore(now)
                    || !DEDUCTIBLE.contains(submission.getStatus())) {
                continue;
            }
            int debit = RatingEngine.computeDebit(
                    student.getAcademicRating(),
                    submission.getArDebited(),
                    submission.getCheckpoint().getCost(),
                    amount,
                    true
            );
            if (debit <= 0) {
                continue;
            }
            student.setAcademicRating(student.getAcademicRating() - debit);
            studentProfileRepository.save(student);
            submission.setArDebited(submission.getArDebited() + debit);
            submission.setStatus(SubmissionStatus.OVERDUE);
            submission.setUpdatedAt(now);
            submissionRepository.save(submission);

            PointLedgerEntry entry = new PointLedgerEntry();
            entry.setStudentProfile(student);
            entry.setCurrency(PointCurrency.AR);
            entry.setAmount(-debit);
            entry.setReason("Просроченная работа «" + submission.getCheckpoint().getTitle() + "»");
            entry.setRefType("CHECKPOINT");
            entry.setRefId(submission.getCheckpoint().getId());
            ledgerRepository.save(entry);

            debitedStudents++;
            totalDebited += debit;
            log.info("AR deduction: studentProfileId={}, checkpointId={}, debit={}, arDebited={}/{}, ar={}",
                    student.getId(), submission.getCheckpoint().getId(), debit,
                    submission.getArDebited(), submission.getCheckpoint().getCost(), student.getAcademicRating());
        }
        return new DeductionResult(markedOverdue, debitedStudents, totalDebited);
    }

    public record DeductionResult(int markedOverdue, int debitedStudents, int totalDebited) {
    }
}
