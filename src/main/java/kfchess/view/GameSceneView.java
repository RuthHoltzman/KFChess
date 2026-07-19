package kfchess.view;

import kfchess.engine.GameSnapshot;
import kfchess.model.PieceColor;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

/**
 * שכבת התצוגה העליונה: מרכיבה קנבס אחד גדול מ-3 חלקים -
 * פאנל השחקן הלבן (שמאל) | הלוח עצמו (כפי שמצייר BoardView) | פאנל השחקן השחור (ימין) -
 * ורק היא קוראת ל-show() בפועל. כך BoardView נשאר אחראי אך ורק על
 * ציור הלוח/הכלים, ו-SidePanelView אחראי אך ורק על ציור פאנל שחקן בודד -
 * כל אחד "יודע" מעט ככל האפשר, בהתאם לסגנון שאר הפרויקט.
 * <p>
 * זו גם השכבה שאחראית על מסך "Game Over" (כותרת המנצח + כפתור Restart) -
 * הוא מצויר ישירות על-גבי קנבס הלוח (boardCanvas), *לפני* שהוא מורכב
 * לתוך הסצנה המלאה, כדי שקואורדינטות הכפתור (restartButtonBounds) יהיו
 * תמיד ביחס ללוח בלבד - בדיוק כמו קואורדינטות העכבר שמגיעות מ-GameWindowMain
 * אחרי שכבר הוחסר מהן boardOffsetX().
 */
public class GameSceneView {

    private static final Color OUTER_BACKGROUND = new Color(30, 30, 30);
    private static final Color OVERLAY_BACKGROUND = new Color(0, 0, 0, 150);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color BUTTON_COLOR = new Color(46, 139, 87);
    private static final Color BUTTON_BORDER_COLOR = Color.WHITE;
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;

    private static final int TITLE_FONT_SIZE = 42;
    private static final int SUBTITLE_FONT_SIZE = 20;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 56;
    private static final int BUTTON_FONT_SIZE = 22;

    private final BoardView boardView;
    private final SidePanelView sidePanelView;
    private final int boardWidthPx;
    private final int boardHeightPx;

    public GameSceneView(BoardView boardView, int boardWidthPx, int boardHeightPx, int panelWidth) {
        this.boardView = boardView;
        this.sidePanelView = new SidePanelView(panelWidth);
        this.boardWidthPx = boardWidthPx;
        this.boardHeightPx = boardHeightPx;
    }

    /** ה-X שבו הלוח מתחיל בתוך הקנבס המורכב - קלט העכבר צריך להתאים אליו. */
    public int boardOffsetX() {
        return sidePanelView.panelWidth();
    }

    public int totalWidthPx() {
        return sidePanelView.panelWidth() * 2 + boardWidthPx;
    }

    public int totalHeightPx() {
        return boardHeightPx;
    }

    /**
     * מיקום/גודל כפתור ה-Restart, ביחס ללוח בלבד (לא לכל הסצנה) - כדי
     * ש-GameWindowMain יוכל לבדוק אם קליק (אחרי החסרת boardOffsetX) נפל
     * בתוכו, בלי לשכפל את המספרים במקום נוסף.
     */
    public Rectangle restartButtonBounds() {
        int x = (boardWidthPx - BUTTON_WIDTH) / 2;
        int y = boardHeightPx / 2 + 30;
        return new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    public void render(GameSnapshot snapshot) {
        Img scene = new Img().newCanvas(totalWidthPx(), totalHeightPx(), OUTER_BACKGROUND);

        Img boardCanvas = boardView.render(snapshot);
        if (snapshot.gameOver()) {
            drawGameOverOverlay(boardCanvas, snapshot.winner());
        }
        boardCanvas.drawOn(scene, boardOffsetX(), 0);

        sidePanelView.draw(scene, 0, totalHeightPx(),
                PieceColor.WHITE,
                snapshot.scores().getOrDefault(PieceColor.WHITE, 0),
                snapshot.moveLog().getOrDefault(PieceColor.WHITE, List.of()));

        sidePanelView.draw(scene, boardOffsetX() + boardWidthPx, totalHeightPx(),
                PieceColor.BLACK,
                snapshot.scores().getOrDefault(PieceColor.BLACK, 0),
                snapshot.moveLog().getOrDefault(PieceColor.BLACK, List.of()));

        scene.show();
    }

    /** מציירת מסך "נגמר המשחק": רקע כהה חצי-שקוף, כותרת עם שם המנצח, וכפתור Restart. */
    private void drawGameOverOverlay(Img boardCanvas, String winner) {
        boardCanvas.fillRect(0, 0, boardWidthPx, boardHeightPx, OVERLAY_BACKGROUND);

        int centerX = boardWidthPx / 2;
        int titleBaselineY = boardHeightPx / 2 - 50;

        String title = winner == null ? "Game Over" : (winner + " Wins!");
        int titleWidth = boardCanvas.textWidth(title, TITLE_FONT_SIZE, true);
        boardCanvas.drawText(title, centerX - titleWidth / 2, titleBaselineY, TITLE_FONT_SIZE, TITLE_COLOR, true);

        String subtitle = "Game Over";
        int subtitleWidth = boardCanvas.textWidth(subtitle, SUBTITLE_FONT_SIZE, false);
        boardCanvas.drawText(subtitle, centerX - subtitleWidth / 2, titleBaselineY + 30,
                SUBTITLE_FONT_SIZE, TITLE_COLOR, false);

        Rectangle button = restartButtonBounds();
        boardCanvas.fillRect(button.x, button.y, button.width, button.height, BUTTON_COLOR);
        boardCanvas.drawRect(button.x, button.y, button.width, button.height, BUTTON_BORDER_COLOR, 2);

        String buttonText = "Restart";
        int buttonTextWidth = boardCanvas.textWidth(buttonText, BUTTON_FONT_SIZE, true);
        int buttonTextX = button.x + (button.width - buttonTextWidth) / 2;
        int buttonTextY = button.y + button.height / 2 + BUTTON_FONT_SIZE / 3;
        boardCanvas.drawText(buttonText, buttonTextX, buttonTextY, BUTTON_FONT_SIZE, BUTTON_TEXT_COLOR, true);
    }
}
