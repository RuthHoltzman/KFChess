package kfchess.model;

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

    public static PieceKind fromCode(char code) {
        for (PieceKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown piece code: " + code);
    }

    public static boolean isValidCode(char code) {
        for (PieceKind kind : values()) {
            if (kind.code == code) {
                return true;
            }
        }
        return false;
    }
}
