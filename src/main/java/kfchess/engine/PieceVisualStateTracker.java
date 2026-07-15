package kfchess.engine;

import kfchess.model.Piece;
import kfchess.model.PieceState;
import java.util.HashMap;
import java.util.Map;

public class PieceVisualStateTracker {

    private static final long SHORT_REST_MS = 500;
    private static final long LONG_REST_MS = 1000;

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
}