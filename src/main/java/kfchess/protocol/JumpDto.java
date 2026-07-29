package kfchess.protocol;

import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Position;


/** Pairs an active jump with its board position, since JumpVisual (like Piece) doesn't know where it is. */
public record JumpDto(Position at, JumpVisual jump) {

    /** Builds a JumpDto from a jump and the position it was found at. */
    public static JumpDto from(JumpVisual jump, Position position) {
        return new JumpDto(position, jump);
    }
}
