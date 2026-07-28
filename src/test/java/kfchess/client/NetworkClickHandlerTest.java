package kfchess.client;

import kfchess.input.BoardMapper;
import kfchess.model.Position;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// client=null ברוב הטסטים כאן בכוונה: resolvePosition() לא נוגע ב-client
// בכלל (הרי בשביל זה היא הוצאה בנפרד - טהורה, בלי רשת), וטסטים ל-handle()
// כאן בודקים רק את הענפים ש"בורחים" *לפני* שנוגעים ב-client (קליק מחוץ
// ללוח, או קליק בזמן gameOver שמפספס את כפתור ה-Restart) - אז NPE על
// client לא אמור לקרות. sceneView **כן** צריך להיות אמיתי (לא null) גם
// בטסטים האלה - handle() שואל אותו restartButtonBounds() בכל קריאה עם
// gameOver=true, גם אם הקליק בסוף מפספס; GameSceneView בטוח לבנות בלי
// לגעת בקבצים בכלל (הבנאי רק שומר path כמחרוזת, ר' תיעוד BoardView) -
// רק render() בפועל היה קורא לקובץ, ואף טסט כאן לא קורא ל-render().
class NetworkClickHandlerTest {

    private static final BoardLayout LAYOUT = new BoardLayout(50, 400, 240, 0);

    private final GameSceneView sceneView = new GameSceneView(new BoardView("unused"), 240);
    private final NetworkClickHandler handler = new NetworkClickHandler(null, new BoardMapper(), sceneView);

    @Test
    void resolvePosition_clickInsideBoard_returnsCorrectPosition() {
        // פיקסל (240+75, 125) - תא (2,1) בגודל 50: row = 125/50 = 2, col = 75/50 = 1
        Optional<Position> resolved = handler.resolvePosition(240 + 75, 125, LAYOUT);
        assertEquals(Optional.of(new Position(2, 1)), resolved);
    }

    @Test
    void resolvePosition_clickAtTopLeftCorner_returnsOrigin() {
        assertEquals(Optional.of(new Position(0, 0)), handler.resolvePosition(240, 0, LAYOUT));
    }

    @Test
    void resolvePosition_clickInSidePanel_returnsEmpty() {
        // פיקסל x=100 קטן מ-offsetX=240 - זה בפאנל השחקן הלבן, לא על הלוח
        assertTrue(handler.resolvePosition(100, 100, LAYOUT).isEmpty());
    }

    @Test
    void resolvePosition_clickPastRightEdgeOfBoard_returnsEmpty() {
        // offsetX(240) + boardPixelSize(400) = 640 - כל מה שמעבר לזה מחוץ ללוח
        assertTrue(handler.resolvePosition(640, 100, LAYOUT).isEmpty());
    }

    @Test
    void resolvePosition_clickAboveBoard_returnsEmpty() {
        assertTrue(handler.resolvePosition(300, -1, LAYOUT).isEmpty());
    }

    @Test
    void resolvePosition_clickBelowBoard_returnsEmpty() {
        assertTrue(handler.resolvePosition(300, 400, LAYOUT).isEmpty());
    }

    @Test
    void handle_gameOverAndClickMissesRestartButton_doesNothingAndDoesNotTouchClient() {
        // (300,100) הופך ל-boardX=60,boardY=100 (LAYOUT.offsetX()=240) - מחוץ
        // לגובה של הכפתור המחושב (y בטווח 30-86 לפני כל render(), ר' תיעוד
        // restartButtonBounds()). client=null - אם handle() בכל זאת היה
        // "פוגע" בכפתור ומנסה client.sendRestart(), זו הייתה נופלת ב-NPE.
        handler.handle(300, 100, LAYOUT, true, false);
    }

    @Test
    void handle_gameOverAndClickHitsRestartButton_sendsRestartToServer() throws URISyntaxException {
        // בונה handler נפרד עם RecordingGameClient אמיתי (לא null) - כי כאן
        // אנחנו כן מצפים ש-sendRestart() ייקרא, וצריך "מרגל" שיודע לרשום את זה.
        RecordingGameClient client = new RecordingGameClient();
        NetworkClickHandler handlerWithClient = new NetworkClickHandler(client, new BoardMapper(), sceneView);

        // הפיקסל מחושב מתוך restartButtonBounds() בפועל (לא מספר קסם קבוע) -
        // כך שהטסט לא ייתלה בקבוע BUTTON_WIDTH/HEIGHT הפנימי של GameSceneView
        // אם הוא ישתנה בעתיד; ר' אותה גישה ב-restart_bothSidesRequest... ב-GameSessionTest.
        Rectangle button = sceneView.restartButtonBounds();
        int pixelX = LAYOUT.offsetX() + button.x + button.width / 2;
        int pixelY = LAYOUT.offsetY() + button.y + button.height / 2;

        handlerWithClient.handle(pixelX, pixelY, LAYOUT, true, false);

        assertTrue(client.restartSent);
    }

    @Test
    void handle_clickOutsideBoardWhileGameStillRunning_doesNothingAndDoesNotTouchClient() {
        // גם כאן client=null - resolvePosition ריק אמור למנוע כל נגיעה ב-client
        handler.handle(100, 100, LAYOUT, false, false);
    }
}
