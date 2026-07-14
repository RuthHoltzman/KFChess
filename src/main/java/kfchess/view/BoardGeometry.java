package kfchess.view;

import kfchess.model.Position;
import java.awt.Point;

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
    
    public Point cellToPixel(Position pos) {
        // TODO: להשתמש ב-pos.row() ו-pos.col()
        int x = pos.col() * cellWidth;
        int y = pos.row() * cellHeight;
        return new Point(x, y);
    }

}