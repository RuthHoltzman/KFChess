package kfchess.net;

import kfchess.model.PieceColor;

import java.util.Optional;

/**
 * התפקיד של חיבור WebSocket בתוך GameSession נתון - רחב יותר מ-PieceColor
 * כי לצופה (SPECTATOR) אין צבע כלל. הראשון שמתחבר ל-GameSession מקבל
 * WHITE, השני BLACK, כל השאר SPECTATOR (ר' GameSession.assignRole).
 */
public enum ClientRole {
    WHITE,
    BLACK,
    SPECTATOR;

    /** ממיר לצבע כלי בפועל - ריק עבור SPECTATOR, כי אין לו כלים לשלוט בהם. */
    public Optional<PieceColor> toPieceColor() {
        return switch (this) {
            case WHITE -> Optional.of(PieceColor.WHITE);
            case BLACK -> Optional.of(PieceColor.BLACK);
            case SPECTATOR -> Optional.empty();
        };
    }
}
