package kfchess.engine;

import kfchess.model.Piece;
import kfchess.model.PieceState;
import java.util.HashMap;
import java.util.Map;

public class PieceVisualStateTracker {

    // משכי המנוחה עצמם (כמה זמן שעון החול נמשך) הם עכשיו מוגדרים במקום
    // אחד בלבד - GameEngine - כי הם גם קובעים בפועל כמה זמן הכלי חסום
    // מפעולה (לא רק כמה זמן מציירים עליו אנימציה). כך אי אפשר להגיע
    // למצב שבו הוויזואל והלוגיקה "מתפצלים" ומראים משכי זמן שונים.
    private static final long SHORT_REST_MS = GameEngine.SHORT_REST_DURATION_MS;
    private static final long LONG_REST_MS = GameEngine.LONG_REST_DURATION_MS;

    private static class Entry {
        PieceState lastKnownLogicalState;
        PieceVisualState visualState;
        long visualStateEnteredAt;
    }

    private final Map<Piece, Entry> entries = new HashMap<>();

    // נקרא פעם בכל render tick, לכל כלי, עם "עכשיו" (אותו elapsedMillis שכבר יש לך)
public PieceVisualState resolve(Piece piece, long now) {
    Entry entry = entries.computeIfAbsent(piece, p -> {
        Entry e = new Entry();
        e.lastKnownLogicalState = p.state();
        e.visualState = PieceVisualState.IDLE;
        e.visualStateEnteredAt = now;
        return e;
    });

    PieceState currentLogicalState = piece.state();

    // TODO 1: זיהוי מעבר לוגי (מה-tick הקודם להווה)
    if (currentLogicalState != entry.lastKnownLogicalState) {
        if (entry.lastKnownLogicalState == PieceState.IN_TRANSIT
                && currentLogicalState == PieceState.IDLE) {
            entry.visualState = PieceVisualState.SHORT_REST;
            entry.visualStateEnteredAt = now;
        } else if (entry.lastKnownLogicalState == PieceState.JUMPING
                && currentLogicalState == PieceState.IDLE) {
            entry.visualState = PieceVisualState.LONG_REST;
            entry.visualStateEnteredAt = now;
        }
        entry.lastKnownLogicalState = currentLogicalState;
    }

    // TODO 2: מצבים "רגילים" שתמיד עוקבים ישירות אחרי הלוגיקה
    if (currentLogicalState == PieceState.IN_TRANSIT) {
        entry.visualState = PieceVisualState.MOVING;
    } else if (currentLogicalState == PieceState.JUMPING) {
        entry.visualState = PieceVisualState.JUMPING;
    }

    // TODO 3: פקיעת מנוחה (רק אם אנחנו כרגע בתוכה)
    if (entry.visualState == PieceVisualState.SHORT_REST
            && now - entry.visualStateEnteredAt >= SHORT_REST_MS) {
        entry.visualState = PieceVisualState.IDLE;
        entry.visualStateEnteredAt = now;
    } else if (entry.visualState == PieceVisualState.LONG_REST
            && now - entry.visualStateEnteredAt >= LONG_REST_MS) {
        entry.visualState = PieceVisualState.IDLE;
        entry.visualStateEnteredAt = now;
    }

    return entry.visualState;
}

    // כמה זמן אנחנו כבר במצב הוויזואלי הנוכחי - נחוץ לחישוב frame index נכון (state-relative, לא global!)
    public long elapsedInCurrentVisualState(Piece piece, long now) {
        Entry entry = entries.get(piece);
        return entry == null ? 0 : now - entry.visualStateEnteredAt;
    }

    /**
     * שבר ההתקדמות (0..1) בתוך מנוחה (SHORT_REST/LONG_REST).
     * 0 = בדיוק נכנס למנוחה, 1 = המנוחה עומדת להסתיים.
     * מחוץ למנוחה מחזיר 0 - שכבת ה-UI פשוט לא תצייר את אפקט "שעון החול".
     */
    public double restProgress(Piece piece, long now) {
        Entry entry = entries.get(piece);
        if (entry == null) {
            return 0;
        }
        long totalMs = switch (entry.visualState) {
            case SHORT_REST -> SHORT_REST_MS;
            case LONG_REST -> LONG_REST_MS;
            default -> 0;
        };
        if (totalMs <= 0) {
            return 0;
        }
        long elapsed = now - entry.visualStateEnteredAt;
        return Math.max(0.0, Math.min(1.0, elapsed / (double) totalMs));
    }
}