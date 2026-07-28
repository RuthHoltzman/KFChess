package kfchess.engine.snapshot;

/** The animation state a piece is currently shown in, derived from its logical PieceState. */
public enum PieceVisualState {
    IDLE,
    MOVING,
    JUMPING,
    SHORT_REST,
    LONG_REST
}