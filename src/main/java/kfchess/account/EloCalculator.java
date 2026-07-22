package kfchess.account;

/**
 * נוסחת ELO הסטנדרטית (אותה נוסחה שמשמשת בשחמט תחרותי אמיתי) - מחלקה
 * טהורה לגמרי (בלי SQLite/רשת) כדי שהחישוב עצמו יהיה נבדק בנפרד
 * מ"מי בדיוק ניצח ואיך זה מגיע לכאן" (זה תפקידו של GameSession).
 * K_FACTOR קבוע (32) - כמות הנקודות המקסימלית שיכולה לעבור בין שני
 * שחקנים במשחק בודד; 32 הוא ברירת המחדל הנפוצה ביותר לשחקנים חדשים/
 * עונתיים (רות אישרה במפורש, לעומת 16 השמרני יותר).
 */
public final class EloCalculator {

    private static final int K_FACTOR = 32;

    private EloCalculator() {
    }

    /**
     * מחשבת את הדירוגים החדשים אחרי משחק בודד עם מנצח/ת ברור/ה (אין תיקו
     * במנוע הזה - סיום משחק הוא תמיד לכידת מלך). מחזירה מערך בגודל 2:
     * [0]=דירוג המנצח/ת החדש, [1]=דירוג המפסיד/ה החדש.
     */
    public static int[] applyResult(int winnerElo, int loserElo) {
        double expectedWinner = expectedScore(winnerElo, loserElo);
        double expectedLoser = 1.0 - expectedWinner;

        int newWinnerElo = (int) Math.round(winnerElo + K_FACTOR * (1.0 - expectedWinner));
        int newLoserElo = (int) Math.round(loserElo + K_FACTOR * (0.0 - expectedLoser));
        return new int[]{newWinnerElo, newLoserElo};
    }

    // ההסתברות הצפויה ש-"playerElo" ינצח מול "opponentElo", לפי הנוסחה
    // הסטנדרטית - ככל שהפער גדול יותר לטובת playerElo, כך התוצאה קרובה יותר ל-1.
    private static double expectedScore(int playerElo, int opponentElo) {
        return 1.0 / (1.0 + Math.pow(10, (opponentElo - playerElo) / 400.0));
    }
}
