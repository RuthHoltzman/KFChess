package kfchess.model;

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

    public static PieceColor fromCode(char code) {
        for (PieceColor color : values()) {
            if (color.code == code) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color code: " + code);
    }

    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
