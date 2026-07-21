package kfchess.net;

import kfchess.model.Position;

/** ייצוג JSON שטוח (row/col) של kfchess.model.Position, לשימוש בפרוטוקול בלבד. */
public class PositionDto {

    private final int row;
    private final int col;

    public PositionDto(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public static PositionDto from(Position position) {
        return new PositionDto(position.row(), position.col());
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }
}
