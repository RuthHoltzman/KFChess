package kfchess.input;

import kfchess.model.Position;

/**
 * ממפה קואורדינטות פיקסלים (כפי שמגיעות מפקודות click/jump) למיקום
 * לוגי על הלוח. גודל המשבצת בפיקסלים עכשיו *לא* קבוע בבנאי - הוא
 * פרמטר של pixelToPosition בכל קריאה, כי גודל התא יכול להשתנות בין
 * קריאה לקריאה (שינוי גודל חלון) - ל-BoardMapper עצמו אין שום זיכרון-
 * מצב, אז אין סיבה לקבע מספר בקונסטרוקטור שעלול "להתיישן".
 * <p>
 * cellWidth ו-cellHeight מתקבלים כאן *בנפרד* בכוונה (לא cellSize יחיד) -
 * כי כשהחלון לא ריבועי בדיוק, רוחב התא וגובה התא הם שני מספרים שונים.
 * שימוש במספר אחד לשניהם היה גורם לשגיאת מיפוי בציר Y שמצטברת ככל
 * שיורדים בלוח (בדיוק התסמין של "שורות תחתונות נקלטות שורה אחת מעל").
 */
public class BoardMapper {

    public Position pixelToPosition(int x, int y, int cellWidth, int cellHeight) {
        return new Position(y / cellHeight, x / cellWidth);
    }
}
