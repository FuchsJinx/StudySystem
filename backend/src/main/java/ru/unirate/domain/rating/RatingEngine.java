package ru.unirate.domain.rating;

/**
 * Pure AR rules — unit-tested without Spring. Spec: docs/spec/03-rating-rules.md
 */
public final class RatingEngine {

    private RatingEngine() {}

    public static int clamp(int ar, int arMax) {
        return Math.max(0, Math.min(arMax, ar));
    }

    public static int applySemesterBonus(int currentAr, int bonus, int arMax) {
        return clamp(currentAr + bonus, arMax);
    }

    /**
     * @return amount actually debited (0 if nothing to debit)
     */
    public static int computeDebit(
            int currentAr,
            int alreadyDebited,
            int cost,
            int deductAmount,
            boolean deductionActive
    ) {
        if (!deductionActive) {
            return 0;
        }
        int remainingCost = cost - alreadyDebited;
        if (remainingCost <= 0 || currentAr <= 0) {
            return 0;
        }
        return Math.min(deductAmount, Math.min(remainingCost, currentAr));
    }

    public static boolean isDeductionActive(SubmissionPhase phase, boolean deadlinePassed) {
        return deadlinePassed && (
                phase == SubmissionPhase.IN_PROGRESS
                        || phase == SubmissionPhase.OVERDUE
                        || phase == SubmissionPhase.RETURNED
        );
    }

    public enum SubmissionPhase {
        IN_PROGRESS, SUBMITTED, OVERDUE, ACCEPTED, RETURNED
    }
}
