package kfchess.realtime;

/**
 * שעון המשחק. הופרד לקלאס עצמאי כדי ש-GameEngine לא יחזיק בעצמו
 * משתנה gameClock גולמי, ובעיקר כדי שיהיה ניתן להזריק שעון מדומה
 * (fake clock) בבדיקות יחידה - למשל להריץ "advance(5000)" ולבדוק
 * שמהלך הסתיים, בלי לבצע Thread.sleep אמיתי.
 */
public class RaelTime {

    private long currentTime = 0;

    public long now() {
        return currentTime;
    }

    public void advance(long milliseconds) {
        if (milliseconds > 0) {
            currentTime += milliseconds;
        }
    }
}
