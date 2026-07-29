package kfchess.view.layout;

import kfchess.view.Img;

import java.awt.Dimension;

/** Keeps on-screen board geometry in one testable place, separate from the Swing window and the drawing code. */
public final class BoardLayoutCalculator {

    private static final int MIN_CELL_SIZE = 20;

    private BoardLayoutCalculator() {
    }

    /** Every number describing where things sit on screen - computed once here, then passed to rendering and click handling. */
    public record BoardLayout(int cellSize, int boardPixelSize, int offsetX, int offsetY) {}

    /** Sizes the board as a centered square in the space left between the two side panels, letterboxing any excess. */
    public static BoardLayout computeLayout(Dimension content, int cols, int rows, int sidePanelWidth) {
        int middleWidth = Math.max(1, content.width - sidePanelWidth * 2);
        int middleHeight = Math.max(1, content.height);
        int squareRawSize = Math.min(middleWidth, middleHeight);

        int cellSize = Math.max(MIN_CELL_SIZE, squareRawSize / Math.max(cols, rows));
        int boardPixelSize = cellSize * Math.max(cols, rows);

        int offsetX = sidePanelWidth + (middleWidth - boardPixelSize) / 2;
        int offsetY = (middleHeight - boardPixelSize) / 2;
        return new BoardLayout(cellSize, boardPixelSize, offsetX, offsetY);
    }

    /** The window's current content size, or a fixed starting size if the window hasn't been created yet. */
    public static Dimension currentContentSize(Img windowAnchor, int sidePanelWidth, int initialCellSize) {
        if (!windowAnchor.isReady()) {
            return new Dimension(sidePanelWidth * 2 + initialCellSize * 8, initialCellSize * 8);
        }
        return windowAnchor.contentSize();
    }
}
