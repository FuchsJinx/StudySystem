package ru.unirate.domain.budget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetPolicyTest {

    @Test
    void participationLevels() {
        assertEquals(25, BudgetPolicy.participationPoints(100, 25));
        assertEquals(50, BudgetPolicy.participationPoints(100, 50));
    }

    @Test
    void budgetGate() {
        assertTrue(BudgetPolicy.canSpend(900, 100, 1000));
        assertFalse(BudgetPolicy.canSpend(901, 100, 1000));
    }
}
