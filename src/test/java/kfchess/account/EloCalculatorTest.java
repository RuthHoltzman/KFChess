package kfchess.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EloCalculatorTest {

    @Test
    void applyResult_equalRatings_bothChangeBySameAmount() {
        // ציפייה שווה (0.5) לשני הצדדים - K=32 * 0.5 = 16 נקודות בכל כיוון
        int[] result = EloCalculator.applyResult(1200, 1200);
        assertArrayEquals(new int[]{1216, 1184}, result);
    }

    @Test
    void applyResult_favoriteWins_gainsFewPointsOnly() {
        // 1400 מול 1000 - המנצח/ת היה/הייתה כבר צפוי/ה לנצח, אז הרווח קטן
        int[] result = EloCalculator.applyResult(1400, 1000);
        assertArrayEquals(new int[]{1403, 997}, result);
    }

    @Test
    void applyResult_underdogWins_gainsManyPoints() {
        // 1000 מנצח את 1400 - הפתעה גדולה, הרווח/הפסד הרבה יותר משמעותיים
        int[] result = EloCalculator.applyResult(1000, 1400);
        assertArrayEquals(new int[]{1029, 1371}, result);
    }
}
