package ru.unirate.domain.budget;

/** Pure budget rules. Spec: docs/spec/04-activity-rules.md */
public final class BudgetPolicy {

    private BudgetPolicy() {}

    public static boolean canSpend(int spentThisMonth, int awardPoints, int monthlyLimit) {
        return spentThisMonth + awardPoints <= monthlyLimit;
    }

    public static int participationPoints(int maxPoints, int levelPercent) {
        return Math.floorDiv(maxPoints * levelPercent, 100);
    }
}
