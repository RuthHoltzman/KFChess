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

    private final PieceColor color;
    private final PieceKind kind;
    private PieceState state = PieceState.IDLE;

    public Piece(PieceColor color, PieceKind kind) {
        this.color = color;
        this.kind = kind;
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
