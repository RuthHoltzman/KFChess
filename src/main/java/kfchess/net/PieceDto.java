package kfchess.net;

import kfchess.model.Piece;
import kfchess.model.Position;

/** ייצוג JSON שטוח של כלי בודד + מיקומו על הלוח, לשימוש ב-SnapshotMessage. */
public class PieceDto {

    private final String color;
    private final String kind;
    private final String state;
    private final int row;
    private final int col;

    public PieceDto(String color, String kind, String state, int row, int col) {
        this.color = color;
        this.kind = kind;
        this.state = state;
        this.row = row;
        this.col = col;
    }

    public static PieceDto from(Piece piece, Position position) {
        return new PieceDto(
                piece.color().name(),
                piece.kind().name(),
                piece.state().name(),
                position.row(),
                position.col());
    }
}
