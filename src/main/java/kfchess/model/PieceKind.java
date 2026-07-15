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

    /**
     * ערך "קלאסי" של הכלי (כמו בשחמט רגיל) - משמש רק לחישוב הניקוד
     * המוצג ב-UI (view/SidePanelView), אין לו שום השפעה על חוקי המשחק.
     */
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
