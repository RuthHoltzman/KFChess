package kfchess.input;

import kfchess.model.Position;

/**
 * ממפה קואורדינטות פיקסלים (כפי שמגיעות מפקודות click/jump) למיקום
 * לוגי על הלוח. גודל המשבצת בפיקסלים הוא קונפיגורציה (קבוע בבנאי),
 * לא hard-coded בתוך לוגיקת הטיפול בקליק כמו בקוד המקורי.
 */
public class BoardMapper {

    private static final int DEFAULT_CELL_SIZE_PIXELS = 100;

    private final int cellSizeInPixels;

    public BoardMapper(int cellSizeInPixels) {
        this.cellSizeInPixels = cellSizeInPixels;
    }

    public static BoardMapper withDefaultCellSize() {
        return new BoardMapper(DEFAULT_CELL_SIZE_PIXELS);
    }

    public Position pixelToPosition(int x, int y) {
        return new Position(y / cellSizeInPixels, x / cellSizeInPixels);
    }
}
