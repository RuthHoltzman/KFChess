package kfchess.view;

import kfchess.engine.snaoshot.GameSnapshot;
import kfchess.model.PieceColor;

import java.awt.Color;
import java.util.List;

/**
 * שכבת התצוגה העליונה: מרכיבה קנבס אחד גדול מ-3 חלקים -
 * פאנל השחקן הלבן (שמאל) | הלוח עצמו (כפי שמצייר BoardView) | פאנל השחקן השחור (ימין) -
 * ורק היא קוראת ל-show() בפועל. כך BoardView נשאר אחראי אך ורק על
 * ציור הלוח/הכלים, ו-SidePanelView אחראי אך ורק על ציור פאנל שחקן בודד -
 * כל אחד "יודע" מעט ככל האפשר, בהתאם לסגנון שאר הפרויקט.
 */
public class GameSceneView {

    private static final Color OUTER_BACKGROUND = new Color(30, 30, 30);

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

    public void render(GameSnapshot snapshot) {
        Img scene = new Img().newCanvas(totalWidthPx(), totalHeightPx(), OUTER_BACKGROUND);

        Img boardCanvas = boardView.render(snapshot);
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
}
