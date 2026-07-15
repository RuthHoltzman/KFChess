package kfchess;

import kfchess.engine.GameEngine;
import kfchess.engine.snaoshot.GameSnapshot;
import kfchess.engine.snaoshot.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.input.GameController;
import kfchess.io.BoardParser;
import kfchess.model.Board;
import kfchess.model.Game;
import kfchess.model.Position;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;
import kfchess.view.BoardGeometry;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.Img;

import javax.swing.Timer;
import java.util.List;
import java.util.Scanner;

public class GameWindowMain {

    private static final int CELL_SIZE = 100;
    // רוחב פאנל הניקוד/המהלכים של כל שחקן, בפיקסלים, בקצוות הלוח.
    private static final int SIDE_PANEL_WIDTH = 240;

    public static void main(String[] args) {
        String startingBoardText = """
                Board:
                bR bN bB bQ bK bB bN bR
                bP bP bP bP bP bP bP bP
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                .  .  .  .  .  .  .  .
                wP wP wP wP wP wP wP wP
                wR wN wB wQ wK wB wN wR
                """;

        Board board = new BoardParser(new Scanner(startingBoardText)).readBoard();
        Game game = new Game(board);
        GameEngine engine = new GameEngine(game, new RuleEngine(), new RaelTime());

        String boardPath = "src/main/resources/board.png";
        Img boardImgForSizing = new Img().read(boardPath); // רק כדי לקרוא מידות, חד-פעמי
        int boardWidthPx = boardImgForSizing.get().getWidth();
        int boardHeightPx = boardImgForSizing.get().getHeight();
        BoardGeometry geometry = new BoardGeometry(boardWidthPx, boardHeightPx, board.height(), board.width());
        BoardView boardView = new BoardView(boardPath, geometry);
        GameSceneView sceneView = new GameSceneView(boardView, boardWidthPx, boardHeightPx, SIDE_PANEL_WIDTH);
        SnapshotFactory snapshotFactory = new SnapshotFactory(CELL_SIZE, CELL_SIZE);

        BoardMapper mapper = new BoardMapper(CELL_SIZE);
        GameController controller = new GameController(engine, mapper);

        // רינדור ראשון - פותח את החלון ומעלה את ה-EDT
        GameSnapshot firstSnapshot = snapshotFactory.createSnapshot(
                board, engine.now(), null, false, null,
                engine.activeMotions(), List.<Position>of(),
                engine.scores(), engine.moveLog());
        sceneView.render(firstSnapshot);

        // רישום יחיד של מאזיני קליק (שמאל = תזוזה, ימין = קפיצה).
        // חשוב: לא לרשום פעמיים - כל רישום כפול היה גורם לכל קליק
        // להתעבד פעמיים (בחירה כפולה / דריסה של הבחירה מיד אחריה).
        //
        // שים לב: הקנבס המוצג כולל עכשיו את פאנל השחקן הלבן משמאל ללוח,
        // ולכן קואורדינטת ה-X שמגיעה מהעכבר היא ביחס לכל הסצנה (כולל
        // הפאנל) ולא ביחס ללוח בלבד. יש להחסיר את רוחב הפאנל השמאלי
        // (sceneView.boardOffsetX()) לפני שממירים לתא לוגי על הלוח,
        // ולהתעלם מקליקים שנפלו בתוך אחד הפאנלים (מחוץ לתחום הלוח).
        javax.swing.SwingUtilities.invokeLater(() -> {
            boardImgForSizing.onClick((pixelX, pixelY) -> {
                int boardX = pixelX - sceneView.boardOffsetX();
                if (boardX < 0 || boardX >= boardWidthPx) {
                    return; // קליק בתוך אחד הפאנלים - לא על הלוח
                }
                controller.click(boardX, pixelY);
            });
            boardImgForSizing.onRightClick((pixelX, pixelY) -> {
                int boardX = pixelX - sceneView.boardOffsetX();
                if (boardX < 0 || boardX >= boardWidthPx) {
                    return;
                }
                controller.rightClick(boardX, pixelY);
            });
        });

        long[] previousTimeNanos = { System.nanoTime() };
        Timer timer = new Timer(16, e -> {
            long now = System.nanoTime();
            long elapsedMillis = (now - previousTimeNanos[0]) / 1_000_000;
            previousTimeNanos[0] = now;

            engine.handleWait(elapsedMillis);

            Position selected = engine.selectedPosition().orElse(null);
            List<Position> legalMoves = selected == null
                    ? List.<Position>of()
                    : engine.legalMovesFrom(selected);

            GameSnapshot snapshot = snapshotFactory.createSnapshot(
                    board,
                    engine.now(),
                    selected,
                    engine.isGameOver(),
                    null,
                    engine.activeMotions(),
                    legalMoves,
                    engine.scores(),
                    engine.moveLog());
            sceneView.render(snapshot);
        });
        timer.start();
    }
}
