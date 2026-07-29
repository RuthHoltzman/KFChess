package kfchess.model;

/** A player's side: WHITE or BLACK. */
public enum PieceColor {
    WHITE('w'),
    BLACK('b');

    private final char code;

    PieceColor(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    /** Looks up the color by its single-char code (used in board-text and piece ids). */
    public static PieceColor fromCode(char code) {
        for (PieceColor color : values()) {
            if (color.code == code) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color code: " + code);
    }

    /** The other color. */
    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
