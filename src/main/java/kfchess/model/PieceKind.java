package kfchess.model;

/** The type of a chess piece. */
public enum PieceKind {
    KING('K'),
    QUEEN('Q'),
    ROOK('R'),
    BISHOP('B'),
    KNIGHT('N'),
    PAWN('P');

    private final char code;

    PieceKind(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    /** Looks up the kind by its single-char code (used in board-text). */
    public static PieceKind fromCode(char code) {
        for (PieceKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown piece code: " + code);
    }

    /** Whether a char is a recognized piece code. */
    public static boolean isValidCode(char code) {
        for (PieceKind kind : values()) {
            if (kind.code == code) {
                return true;
            }
        }
        return false;
    }

    /** Classic chess point value, used only for the score shown in the UI - has no effect on game rules. */
    public int value() {
        return switch (this) {
            case PAWN -> 1;
            case KNIGHT, BISHOP -> 3;
            case ROOK -> 5;
            case QUEEN -> 9;
            case KING -> 0;
        };
    }
}
