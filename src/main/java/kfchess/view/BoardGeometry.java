package kfchess.view;

import kfchess.model.Position;
import java.awt.Point;

/** Cell dimensions for one rendered board, and the board-square -> pixel conversion that follows from them. */
public class BoardGeometry {

    private final int cellWidth;
    private final int cellHeight;
    private final int rows;
    private final int cols;

    public BoardGeometry(int boardWidthPx, int boardHeightPx, int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cellWidth = boardWidthPx / cols;
        this.cellHeight = boardHeightPx / rows;
    }


    public int getCellWidth() {
        return cellWidth;
    }
    public int getCellHeight() {
        return cellHeight;
    }

    public int getRows() {
        return rows;
    }
    public int getCols() {
        return cols;
    }
    
    /** The top-left pixel of the given board square. */
    public Point cellToPixel(Position pos) {
        int x = pos.col() * cellWidth;
        int y = pos.row() * cellHeight;
        return new Point(x, y);
    }

}