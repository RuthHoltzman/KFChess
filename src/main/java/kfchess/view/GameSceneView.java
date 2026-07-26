package kfchess.view;

import kfchess.engine.snapshot.GameSnapshot;
import kfchess.model.PieceColor;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

/**
 * שכבת התצוגה העליונה: מרכיבה קנבס אחד גדול -
 * פאנל השחקן הלבן (מוצמד לקצה השמאלי) | הלוח (ריבועי, ממורכז בשטח
 * שנשאר באמצע) | פאנל השחקן השחור (מוצמד לקצה הימני) -
 * ורק היא קוראת ל-show() בפועל.
 * <p>
 * בכוונה, המחלקה הזו לא מחשבת שום גיאומטריה בעצמה יותר (לא היכן הלוח
 * מתחיל, לא כמה מקום נשאר) - כל המספרים (גודל הלוח, ה-offset שלו)
 * מגיעים כפרמטרים מוכנים מ-NetworkGameWindowMain, שהוא המקום היחיד שבאמת
 * יודע מה גודל החלון האמיתי כרגע. זה לקח משתי באגים קודמים: כל פעם
 * ששני מקומות שונים חישבו את אותו מספר בנפרד (במקום שאחד יחשב ויעביר
 * לשני), הם התבדרו זה מזה וזה יצר בדיוק את הבאגים של "קליק לא במקום".
 * <p>
 * הלוח *תמיד* ריבועי (cellSize זהה לרוחב ולגובה) - זו הסיבה שאין יותר
 * "קצוות שהופכות למלבן": אם החלון עצמו לא ריבועי, פשוט נשאר שוליים
 * ריקים (letterboxing) בציר שיש בו עודף מקום, במקום למתוח את הלוח.
 */
public class GameSceneView {

    private static final Color OUTER_BACKGROUND = new Color(30, 30, 30);
    private static final Color OVERLAY_BACKGROUND = new Color(0, 0, 0, 150);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color BUTTON_COLOR = new Color(46, 139, 87);
    private static final Color BUTTON_BORDER_COLOR = Color.WHITE;
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;
    // כתום-אדמדם, שונה בכוונה מ-OVERLAY_BACKGROUND (שחור) - כדי שהבאנר
    // יבלוט כ"אזהרה" ולא יתבלבל עם מסך ה-Game-Over, למרות שהם אף פעם
    // לא מוצגים בו-זמנית בפועל (ר' תיעוד drawDisconnectBanner).
    private static final Color DISCONNECT_BANNER_BACKGROUND = new Color(120, 40, 20, 210);

    private static final int TITLE_FONT_SIZE = 42;
    private static final int SUBTITLE_FONT_SIZE = 20;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 56;
    private static final int BUTTON_FONT_SIZE = 22;
    private static final int DISCONNECT_BANNER_HEIGHT = 40;
    private static final int DISCONNECT_BANNER_FONT_SIZE = 20;

    private final BoardView boardView;
    private final SidePanelView sidePanelView;

    // "הגודל האחרון שידוע" - מתעדכן בתחילת כל render(). לא זיכרון-מצב
    // אמיתי, רק נוחות כדי ש-restartButtonBounds() (בלי פרמטרים, נקראת
    // גם מחוץ ל-render כדי לבדוק קליק) תדע למה להתייחס.
    private int lastBoardPixelSize;

    public GameSceneView(BoardView boardView, int panelWidth) {
        this.boardView = boardView;
        this.sidePanelView = new SidePanelView(panelWidth);
    }

    /**
     * מיקום/גודל כפתור ה-Restart, ביחס ללוח בלבד (0,0 = הפינה השמאלית-
     * עליונה של הלוח עצמו, לא של כל הסצנה) - נכון תמיד אחרי לפחות
     * render() אחד. הקוד הקורא צריך להחסיר את ה-offset של הלוח (שהוא
     * עצמו מחשב) לפני שהוא בודק קליק מול זה.
     * <p>
     * הערה: כרגע אין קוד שקורא למתודה הזו בפועל (שימשה את מסך המשחק
     * המקומי שהוסר) - נשארה כאן כתשתית מוכנה לכפתור Restart ב-UI
     * הרשת, אם/כשיתווסף.
     */
    public Rectangle restartButtonBounds() {
        int x = (lastBoardPixelSize - BUTTON_WIDTH) / 2;
        int y = lastBoardPixelSize / 2 + 30;
        return new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    /**
     * @param sceneWidthPx   הרוחב הכולל של החלון (הפנימי, לציור) - כולל שני הפאנלים.
     * @param sceneHeightPx  הגובה הכולל של החלון.
     * @param boardPixelSize גודל הלוח בפיקסלים - *ריבוע* אחד (רוחב=גובה תמיד).
     * @param boardOffsetX   היכן הלוח מתחיל בציר X בתוך הסצנה (כבר כולל את הפאנל השמאלי + מירכוז).
     * @param boardOffsetY   היכן הלוח מתחיל בציר Y בתוך הסצנה (מירכוז אנכי אם יש שוליים).
     */
    public void render(GameSnapshot snapshot, int sceneWidthPx, int sceneHeightPx,
                        int boardPixelSize, int boardOffsetX, int boardOffsetY) {
        this.lastBoardPixelSize = boardPixelSize;

        BoardGeometry geometry = new BoardGeometry(
                boardPixelSize, boardPixelSize, snapshot.boardHeightCells(), snapshot.boardWidthCells());

        Img scene = new Img().newCanvas(sceneWidthPx, sceneHeightPx, OUTER_BACKGROUND);

        Img boardCanvas = boardView.render(snapshot, geometry);
        if (snapshot.gameOver()) {
            drawGameOverOverlay(boardCanvas, snapshot.winner(), snapshot.restartRequestedByViewer());
        }
        // בפועל אף פעם לא קורה בו-זמנית עם gameOver (ר' GameSession.resolveExpiredDisconnects -
        // ברגע שחלון החסד פג, gameOver הופך ל-true ו-disconnectSecondsRemaining חוזר ל-null
        // באותו טיק) - אבל אין תלות מפורשת בין שני ה-if-ים כאן בכוונה, כל אחד עצמאי לגמרי
        // לפי מה שה-snapshot בפועל מכיל, ולא לפי הנחה על מה "לא אמור" לקרות יחד.
        if (snapshot.disconnectSecondsRemaining() != null) {
            drawDisconnectBanner(boardCanvas, snapshot.disconnectSecondsRemaining());
        }
        boardCanvas.drawOn(scene, boardOffsetX, boardOffsetY);

        int panelWidth = sidePanelView.panelWidth();
        sidePanelView.draw(scene, 0, sceneHeightPx,
                PieceColor.WHITE,
                snapshot.scores().getOrDefault(PieceColor.WHITE, 0),
                snapshot.moveLog().getOrDefault(PieceColor.WHITE, List.of()));

        sidePanelView.draw(scene, sceneWidthPx - panelWidth, sceneHeightPx,
                PieceColor.BLACK,
                snapshot.scores().getOrDefault(PieceColor.BLACK, 0),
                snapshot.moveLog().getOrDefault(PieceColor.BLACK, List.of()));

        scene.show();
    }

    /**
     * מציירת מסך "נגמר המשחק": רקע כהה חצי-שקוף, כותרת עם שם המנצח,
     * וכפתור Restart. restartRequestedByViewer - האם *הצופה הזה בדיוק*
     * כבר ביקש/ה RESTART (ר' GameSession.applyRestartVote - שני הצדדים
     * צריכים לבקש כדי שהלוח יתאפס בפועל) - אם כן, מציגה "Waiting for
     * opponent..." במקום "Game Over"/"Restart", כדי שהצד שכבר לחץ יידע
     * שהקליק שלו נקלט ולא רק ילחץ שוב ושוב בלי משוב.
     */
    private void drawGameOverOverlay(Img boardCanvas, String winner, boolean restartRequestedByViewer) {
        boardCanvas.fillRect(0, 0, lastBoardPixelSize, lastBoardPixelSize, OVERLAY_BACKGROUND);

        int centerX = lastBoardPixelSize / 2;
        int titleBaselineY = lastBoardPixelSize / 2 - 50;

        String title = winner == null ? "Game Over" : (winner + " Wins!");
        int titleWidth = boardCanvas.textWidth(title, TITLE_FONT_SIZE, true);
        boardCanvas.drawText(title, centerX - titleWidth / 2, titleBaselineY, TITLE_FONT_SIZE, TITLE_COLOR, true);

        String subtitle = restartRequestedByViewer ? "Waiting for opponent..." : "Game Over";
        int subtitleWidth = boardCanvas.textWidth(subtitle, SUBTITLE_FONT_SIZE, false);
        boardCanvas.drawText(subtitle, centerX - subtitleWidth / 2, titleBaselineY + 30,
                SUBTITLE_FONT_SIZE, TITLE_COLOR, false);

        Rectangle button = restartButtonBounds();
        boardCanvas.fillRect(button.x, button.y, button.width, button.height, BUTTON_COLOR);
        boardCanvas.drawRect(button.x, button.y, button.width, button.height, BUTTON_BORDER_COLOR, 2);

        String buttonText = restartRequestedByViewer ? "Waiting..." : "Restart";
        int buttonTextWidth = boardCanvas.textWidth(buttonText, BUTTON_FONT_SIZE, true);
        int buttonTextX = button.x + (button.width - buttonTextWidth) / 2;
        int buttonTextY = button.y + button.height / 2 + BUTTON_FONT_SIZE / 3;
        boardCanvas.drawText(buttonText, buttonTextX, buttonTextY, BUTTON_FONT_SIZE, BUTTON_TEXT_COLOR, true);
    }

    /**
     * מציירת פס אזהרה צר לרוחב הלוח כולו, צמוד לקצה העליון: "Opponent
     * disconnected - Xs to reconnect" - שלב 5 (auto-resign), ר' תיעוד
     * GameSession.pendingDisconnects/DISCONNECT_GRACE_MILLIS. בכוונה
     * *לא* overlay מלא כמו drawGameOverOverlay - המשחק לא נגמר, הלוח
     * עדיין אמור להיראות (קפוא, כי אין tick חדש עד שהניתוק נפתר, אבל
     * לא מוסתר) - רק באנר דק שמסביר *למה* הוא קפוא.
     */
    private void drawDisconnectBanner(Img boardCanvas, int secondsRemaining) {
        boardCanvas.fillRect(0, 0, lastBoardPixelSize, DISCONNECT_BANNER_HEIGHT, DISCONNECT_BANNER_BACKGROUND);

        String text = "Opponent disconnected - " + secondsRemaining + "s to reconnect";
        int textWidth = boardCanvas.textWidth(text, DISCONNECT_BANNER_FONT_SIZE, true);
        int textX = (lastBoardPixelSize - textWidth) / 2;
        int textY = DISCONNECT_BANNER_HEIGHT / 2 + DISCONNECT_BANNER_FONT_SIZE / 3;
        boardCanvas.drawText(text, textX, textY, DISCONNECT_BANNER_FONT_SIZE, TITLE_COLOR, true);
    }
}
