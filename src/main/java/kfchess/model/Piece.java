package kfchess.model;

/** A game piece. It doesn't know its own board position (Board is the single source of truth for that), but it does own and guard its own state (IDLE/IN_TRANSIT/JUMPING). */
public class Piece {

    // Global counter - each piece gets a stable, increasing id once, in the constructor.
    // Needed so the client (ClientSnapshotReconstructor) can recognize "this is the same
    // piece as before" across separate JSON messages, where plain Java object identity
    // is lost on every decode. Has no effect on local game logic - just an id field.
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

    /** Starts a move; fails if the piece isn't idle. */
    public void markInTransit() {
        requireState(PieceState.IDLE, "start a move");
        state = PieceState.IN_TRANSIT;
    }

    /** Ends a move or jump, returning the piece to idle. */
    public void markArrived() {
        state = PieceState.IDLE;
    }

    /** Starts a jump; fails if the piece isn't idle. */
    public void markJumping() {
        requireState(PieceState.IDLE, "start a jump");
        state = PieceState.JUMPING;
    }

    /** Ends a jump early/expires it, returning the piece to idle - no-op if it wasn't jumping. */
    public void markJumpEnded() {
        if (state == PieceState.JUMPING) {
            state = PieceState.IDLE;
        }
    }

    /** Guards a state transition: throws if the piece isn't currently in the required state. */
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
