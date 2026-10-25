package ru.unirate.domain.rating;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.unirate.domain.rating.RatingEngine.SubmissionPhase;
import static ru.unirate.domain.rating.RatingEngine.computeDebit;
import static ru.unirate.domain.rating.RatingEngine.applySemesterBonus;
import static ru.unirate.domain.rating.RatingEngine.isDeductionActive;

class RatingEngineTest {

    @Test
    void tickDebitsTwenty() {
        assertEquals(20, computeDebit(1000, 0, 100, 20, true));
    }

    @Test
    void stopsAtCost() {
        assertEquals(10, computeDebit(1000, 90, 100, 20, true));
        assertEquals(0, computeDebit(1000, 100, 100, 20, true));
    }

    @Test
    void floorAtZero() {
        assertEquals(15, computeDebit(15, 0, 100, 20, true));
    }

    @Test
    void noDebitWhenSubmitted() {
        assertFalse(isDeductionActive(SubmissionPhase.SUBMITTED, true));
        assertEquals(0, computeDebit(1000, 0, 100, 20, false));
    }

    @Test
    void resumesOnReturnedAfterDeadline() {
        assertTrue(isDeductionActive(SubmissionPhase.RETURNED, true));
        assertFalse(isDeductionActive(SubmissionPhase.RETURNED, false));
    }

    @Test
    void semesterBonusCapsAtMax() {
        assertEquals(1000, applySemesterBonus(900, 200, 1000));
        assertEquals(900, applySemesterBonus(700, 200, 1000));
    }
}
