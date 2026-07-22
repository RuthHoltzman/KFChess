package texttests;

import kfchess.input.BoardMapper;
import kfchess.model.Position;
import kfchess.net.client.NetworkClickHandler;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// client=null בכל הטסטים כאן בכוונה: resolvePosition() לא נוגע ב-client
// בכלל (הרי בשביל זה היא הוצאה בנפרד - טהורה, בלי רשת), וטסטים ל-handle()
// כאן בודקים רק את הענפים ש"בורחים" *לפני* שנוגעים ב-client (gameOver או
// קליק מחוץ ללוח) - אז NPE על client לא אמור לקרות. הענף שבו handle()
// באמת שולח לשרת לא נבדק כאן (יידרש GameClient מחובר בפועל - ר' תיעוד
// NetworkClickHandler למה זו "שכבה דקה" לא-נבדקת, כמו HomeScreenMain.connect()).
class NetworkClickHandlerTest {

    private static final BoardLayout LAYOUT = new BoardLayout(50, 400, 240, 0);

    private final NetworkClickHandler handler = new NetworkClickHandler(null, new BoardMapper());

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
    void handle_gameOver_doesNothingAndDoesNotTouchClient() {
        // client=null - אם handle() היה נוגע בו למרות gameOver=true, זו הייתה נופלת ב-NPE
        handler.handle(300, 100, LAYOUT, true, false);
    }

    @Test
    void handle_clickOutsideBoardWhileGameStillRunning_doesNothingAndDoesNotTouchClient() {
        // גם כאן client=null - resolvePosition ריק אמור למנוע כל נגיעה ב-client
        handler.handle(100, 100, LAYOUT, false, false);
    }
}
