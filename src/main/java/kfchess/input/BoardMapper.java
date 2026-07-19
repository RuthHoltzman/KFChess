package kfchess.input;

import kfchess.model.Position;

/**
 * ממפה קואורדינטות פיקסלים (כפי שמגיעות מפקודות click/jump) למיקום
 * לוגי על הלוח. גודל המשבצת בפיקסלים עכשיו *לא* קבוע בבנאי - הוא
 * פרמטר של pixelToPosition בכל קריאה, כי גודל התא יכול להשתנות בין
 * קריאה לקריאה (שינוי גודל חלון) - ל-BoardMapper עצמו אין שום זיכרון-
 * מצב, אז אין סיבה לקבע מספר בקונסטרוקטור שעלול "להתיישן".
 */
public class BoardMapper {

    public Position pixelToPosition(int x, int y, int cellSizeInPixels) {
        return new Position(y / cellSizeInPixels, x / cellSizeInPixels);
    }
}
