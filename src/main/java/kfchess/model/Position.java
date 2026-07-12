package kfchess.model;

import java.util.Objects;

public final class Position {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    public Position offset(int deltaRow, int deltaCol) {
        return new Position(row + deltaRow, col + deltaCol);
    }

    public boolean isWithinBounds(int height, int width) {
        return row >= 0 && row < height && col >= 0 && col < width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "Position[row=" + row + ", col=" + col + "]";
    }
}