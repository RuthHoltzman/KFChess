package kfchess.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EloCalculatorTest {

    @Test
    void applyResult_equalRatings_bothChangeBySameAmount() {
        // Equal expectation (0.5) on both sides - K=32 * 0.5 = 16 points each way.
        int[] result = EloCalculator.applyResult(1200, 1200);
        assertArrayEquals(new int[]{1216, 1184}, result);
    }

    @Test
    void applyResult_favoriteWins_gainsFewPointsOnly() {
        // 1400 vs 1000 - the winner was already expected to win, so the gain is small.
        int[] result = EloCalculator.applyResult(1400, 1000);
        assertArrayEquals(new int[]{1403, 997}, result);
    }

    @Test
    void applyResult_underdogWins_gainsManyPoints() {
        // 1000 beats 1400 - a big upset, so the swing is much larger.
        int[] result = EloCalculator.applyResult(1000, 1400);
        assertArrayEquals(new int[]{1029, 1371}, result);
    }
}
