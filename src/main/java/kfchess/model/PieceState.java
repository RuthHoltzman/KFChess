package kfchess.model;

/** The state a single piece can be in; tracked per-piece so multiple pieces can move/jump concurrently. */
public enum PieceState {
    IDLE,
    IN_TRANSIT,
    JUMPING
}
