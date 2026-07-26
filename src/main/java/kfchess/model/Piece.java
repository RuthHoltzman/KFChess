package kfchess.model;

/**
 * ייצוג של כלי במשחק. שימו לב שהכלי "לא יודע" איפה הוא נמצא על הלוח -
 * המיקום הוא אחריות בלעדית של Board (Single Source of Truth אחד),
 * כדי שלא ייווצר מצב של שני מקורות אמת סותרים.
 * <p>
 * הכלי כן אחראי על המצב הפרטי שלו (IDLE / IN_TRANSIT / JUMPING),
 * ושומר על עצמו מפני מעברי מצב לא חוקיים (encapsulation אמיתי -
 * אף מחלקה אחרת לא "דוחפת" ערך לשדה state ישירות).
 */
public class Piece {

    // מונה גלובלי - כל כלי מקבל מזהה עולה, פעם אחת, בבנאי. נחוץ כדי
    // שהלקוח (ר' kfchess.net.client.ClientSnapshotReconstructor) יוכל לזהות
    // "זה אותו כלי שהיה קודם" בין הודעות JSON נפרדות (שבהן זהות אובייקט
    // Java רגילה הולכת לאיבוד בכל פענוח) - בלי מזהה יציב כזה, אין דרך
    // אמינה להבחין בין "כלי המשיך לזוז" ל"כלי חדש נוצר באותו מיקום".
    // לא משפיע על שום לוגיקת משחק מקומית - רק שדה מזהה נוסף.
    private static final java.util.concurrent.atomic.AtomicLong NEXT_ID =
            new java.util.concurrent.atomic.AtomicLong(1);

    private final long id = NEXT_ID.getAndIncrement();
    private final PieceColor color;
    private final PieceKind kind;
    private PieceState state = PieceState.IDLE;

    public Piece(PieceColor color, PieceKind kind) {
        this.color = color;
        this.kind = kind;
    }

    public long id() {
        return id;
    }

    public PieceColor color() {
        return color;
    }

    public PieceKind kind() {
        return kind;
    }

    public PieceState state() {
        return state;
    }

    public boolean isIdle() {
        return state == PieceState.IDLE;
    }

    public boolean isInTransit() {
        return state == PieceState.IN_TRANSIT;
    }

    public boolean isJumping() {
        return state == PieceState.JUMPING;
    }

    public boolean isSameColor(Piece other) {
        return other != null && this.color == other.color;
    }

    public void markInTransit() {
        requireState(PieceState.IDLE, "start a move");
        state = PieceState.IN_TRANSIT;
    }

    public void markArrived() {
        state = PieceState.IDLE;
    }

    public void markJumping() {
        requireState(PieceState.IDLE, "start a jump");
        state = PieceState.JUMPING;
    }

    public void markJumpEnded() {
        if (state == PieceState.JUMPING) {
            state = PieceState.IDLE;
        }
    }

    private void requireState(PieceState expected, String action) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " - piece is currently " + state);
        }
    }

    @Override
    public String toString() {
        return "" + color.code() + kind.code();
    }
}
