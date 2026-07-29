package kfchess.model;

import java.util.Optional;

/** A connected client's role in a PlaySession - wider than PieceColor since a spectator has no color at all. */
public enum ClientRole {
    WHITE,
    BLACK,
    SPECTATOR;

    /** The matching piece color, or empty for SPECTATOR (who has no pieces to control). */
    public Optional<PieceColor> toPieceColor() {
        return switch (this) {
            case WHITE -> Optional.of(PieceColor.WHITE);
            case BLACK -> Optional.of(PieceColor.BLACK);
            case SPECTATOR -> Optional.empty();
        };
    }
}
