package texttests;

import kfchess.view.GameSceneView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GameSceneView בעצמה לא נבדקת ישירות (ציור/Swing, בדיוק כמו קודם - ר'
 * PROGRESS.md) - אבל shortRoomId() טהורה לגמרי (בלי Img/גרפיקה), אז אין
 * סיבה לא לבדוק אותה כמו כל לוגיקה משמעותית אחרת בפרויקט. בקשת רות
 * (עיצוב יותר יפה): מזהי matchmaking ("match-<uuid>") מקוצרים ל-8
 * התווים הראשונים של ה-UUID בלבד, קודי Create/Join לא נוגעים בהם בכלל.
 */
class GameSceneViewTest {

    @Test
    void shortRoomId_matchmakingId_truncatesToFirst8UuidChars() {
        assertEquals("816a33df",
                GameSceneView.shortRoomId("match-816a33df-6e2a-4772-b3e7-09c6768d4150"));
    }

    @Test
    void shortRoomId_createOrJoinShortCode_isUnchanged() {
        assertEquals("AB12CD", GameSceneView.shortRoomId("AB12CD"));
    }

    @Test
    void shortRoomId_defaultRoom_isUnchanged() {
        assertEquals("default", GameSceneView.shortRoomId("default"));
    }

    @Test
    void shortRoomId_nullGameId_returnsQuestionMark() {
        assertEquals("?", GameSceneView.shortRoomId(null));
    }

    @Test
    void shortRoomId_matchmakingIdShorterThan8Chars_returnsWhateverRemains() {
        // מקרה-קצה תיאורטי (UUID אמיתי תמיד ארוך בהרבה מ-8) - לא אמור
        // לזרוק חריגה גם אם החלק שאחרי "match-" קצר מהצפוי.
        assertEquals("ab", GameSceneView.shortRoomId("match-ab"));
    }
}
