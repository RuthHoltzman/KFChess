package kfchess.engine;

import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "כמה זמן כל כלי נעול/קופץ/במנוחה" - כל ה-bookkeeping התלוי-בזמן שקשור
 * לכלי בודד (לא ללוח בכלל, ולא לחוקיות מהלך). הוצא מ-GameEngine כי זו
 * אחריות ברורה ונפרדת מ"מה מותר לזוז" (RuleEngine) ומ"מה קורה כשתנועה
 * מסתיימת" (GameEngine.completeMotion) - שני אלה נשארים ב-GameEngine.
 */
public class PieceTimers {

    private static final long DEFAULT_JUMP_DURATION_MS = 1000;

    // ציבוריים כי PieceVisualStateTracker משתמש באותם ערכים בדיוק כדי
    // שהאנימציה (שעון החול) תמיד תואמת בדיוק את משך הזמן שבו הכלי באמת
    // לא ניתן להזזה - אין שני מקורות אמת לאותו מספר.
    public static final long SHORT_REST_DURATION_MS = 500;  // אחרי הליכה
    public static final long LONG_REST_DURATION_MS = 1000;  // אחרי קפיצה

    private final Map<Piece, Long> jumpEndTimes = new HashMap<>();
    // זמני התחלה של קפיצות פעילות - נשמר במקביל ל-jumpEndTimes (ולא בתוכו),
    // כדי לא לגעת בלוגיקת הקפיצה הקיימת שכבר עובדת נכון; המפה הזו משרתת
    // אך ורק את שכבת התצוגה (חישוב קשת הגובה של הקפיצה ב-SnapshotFactory).
    private final Map<Piece, Long> jumpStartTimes = new HashMap<>();
    // מתי המנוחה הנוכחית של כל כלי (אם יש) מסתיימת - כל עוד now קטן
    // מהערך הזה, הכלי לא זמין לבחירה/תנועה/קפיצה (ר' isAvailableToAct).
    private final Map<Piece, Long> restEndTimes = new HashMap<>();

    /** מתחיל קפיצה עבור כלי: מסמן אותו כ-JUMPING ורושם את זמני ההתחלה/סיום. */
    public void beginJump(Piece piece, long now) {
        piece.markJumping();
        jumpStartTimes.put(piece, now);
        jumpEndTimes.put(piece, now + DEFAULT_JUMP_DURATION_MS);
    }

    /** מתחיל מנוחה קצרה (אחרי הליכה רגילה) עבור כלי. */
    public void beginShortRest(Piece piece, long now) {
        restEndTimes.put(piece, now + SHORT_REST_DURATION_MS);
    }

    /**
     * האם כלי זמין כרגע לפעולה (בחירה/תנועה/קפיצה): לא רק "IDLE" ברמת
     * המודל, אלא גם לא נמצא כרגע ב"מנוחה" (cooldown) אחרי הליכה/קפיצה
     * קודמת. זה מה שהופך את שעון החול הצהוב מקישוט בלבד לכלל משחק אמיתי.
     */
    public boolean isAvailableToAct(Piece piece, long now) {
        if (!piece.isIdle()) {
            return false;
        }
        Long restEndTime = restEndTimes.get(piece);
        return restEndTime == null || now >= restEndTime;
    }

    /** מסיים קפיצות שפג זמנן: מעביר את הכלי למנוחה ארוכה. */
    public void resolveExpiredJumps(long now) {
        List<Piece> finishedJumpers = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : jumpEndTimes.entrySet()) {
            if (now >= entry.getValue()) {
                finishedJumpers.add(entry.getKey());
            }
        }
        for (Piece piece : finishedJumpers) {
            piece.markJumpEnded();
            jumpEndTimes.remove(piece);
            jumpStartTimes.remove(piece);
            restEndTimes.put(piece, now + LONG_REST_DURATION_MS);
        }
    }

    /** מנקה רשומות מנוחה שכבר פקעו, כדי שהמפה לא תגדל ללא גבול לאורך משחק ארוך. */
    public void purgeExpiredRest(long now) {
        restEndTimes.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    /**
     * כל הקפיצות הפעילות כרגע, עם זמני התחלה/סיום - נחוץ לשכבת ה-UI
     * כדי לצייר קשת גובה (הכלי "עולה" ו"יורד") בזמן הקפיצה.
     */
    public List<JumpVisual> activeJumps() {
        List<JumpVisual> jumps = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : jumpEndTimes.entrySet()) {
            Piece piece = entry.getKey();
            long endTime = entry.getValue();
            long startTime = jumpStartTimes.getOrDefault(piece, endTime - DEFAULT_JUMP_DURATION_MS);
            jumps.add(new JumpVisual(piece, startTime, endTime));
        }
        return jumps;
    }
}
