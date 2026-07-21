package kfchess;

import kfchess.bus.EventBus;
import kfchess.engine.GameEngine;
import kfchess.engine.snapshot.GameSnapshot;
import kfchess.engine.snapshot.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.input.GameController;
import kfchess.io.BoardParser;
import kfchess.model.Board;
import kfchess.model.Game;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.Img;

import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import java.util.Scanner;

public class GameWindowMain {

    private static final int INITIAL_CELL_SIZE = 100;
    private static final int MIN_CELL_SIZE = 20;
    private static final int SIDE_PANEL_WIDTH = 240;

    private static final String STARTING_BOARD_TEXT = """
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

    private static final class GameSession {
        final Board board;
        final Game game;
        final GameEngine engine;
        final GameController controller;
        final SnapshotFactory snapshotFactory;

        GameSession() {
            this.board = new BoardParser(new Scanner(STARTING_BOARD_TEXT)).readBoard();
            this.game = new Game(board);
            this.engine = new GameEngine(game, new RuleEngine(), new RaelTime(), new EventBus());
            this.controller = new GameController(engine, new BoardMapper());
            this.snapshotFactory = new SnapshotFactory();
        }

        GameSnapshot currentSnapshot(int cellSize) {
            Position selected = engine.selectedPosition().orElse(null);
            List<Position> legalMoves = selected == null
                    ? List.of()
                    : engine.legalMovesFrom(selected);
            String winner = engine.winner()
                    .map(color -> color == PieceColor.WHITE ? "White" : "Black")
                    .orElse(null);

            return snapshotFactory.createSnapshot(
                    board,
                    cellSize,
                    cellSize,
                    engine.now(),
                    selected,
                    engine.isGameOver(),
                    winner,
                    engine.activeMotions(),
                    engine.activeJumps(),
                    engine.recentCaptureEffects(),
                    legalMoves,
                    engine.scores(),
                    engine.moveLog());
        }
    }

    /**
     * כל המספרים שקובעים "איפה כל דבר נמצא על המסך" ברגע נתון - מחושבים
     * *במקום אחד בלבד* (computeLayout למטה) ומועברים מוכנים לכל מי שצריך
     * אותם (רינדור, טיפול בקליק). זה בדיוק הלקח משתי הבעיות הקודמות: כל
     * פעם ששני מקומות חישבו משהו דומה בנפרד, הם התבדרו זה מזה.
     */
    private record BoardLayout(int cellSize, int boardPixelSize, int offsetX, int offsetY) {}

    /**
     * הלוח תמיד *ריבועי* - cellSize זהה לרוחב ולגובה, לא שני מספרים
     * נפרדים. אם החלון עצמו לא ריבועי, לוקחים את הצד הקטן מבין השניים
     * (השטח שנשאר באמצע, אחרי הפאנלים) לקביעת גודל הלוח, וממרכזים אותו -
     * כך שנשארים שוליים ריקים בציר שיש בו עודף מקום, במקום למתוח את
     * הלוח למלבן.
     */
    private static BoardLayout computeLayout(Dimension content, int cols, int rows) {
        int middleWidth = Math.max(1, content.width - SIDE_PANEL_WIDTH * 2);
        int middleHeight = Math.max(1, content.height);
        int squareRawSize = Math.min(middleWidth, middleHeight);

        int cellSize = Math.max(MIN_CELL_SIZE, squareRawSize / Math.max(cols, rows));
        int boardPixelSize = cellSize * Math.max(cols, rows);

        int offsetX = SIDE_PANEL_WIDTH + (middleWidth - boardPixelSize) / 2;
        int offsetY = (middleHeight - boardPixelSize) / 2;
        return new BoardLayout(cellSize, boardPixelSize, offsetX, offsetY);
    }

    private static Dimension currentContentSize(Img windowAnchor) {
        if (!windowAnchor.isReady()) {
            return new Dimension(SIDE_PANEL_WIDTH * 2 + INITIAL_CELL_SIZE * 8, INITIAL_CELL_SIZE * 8);
        }
        return windowAnchor.contentSize();
    }

    public static void main(String[] args) {
        GameSession[] session = { new GameSession() };

        BoardView boardView = new BoardView("src/main/resources/board.png");
        GameSceneView sceneView = new GameSceneView(boardView, SIDE_PANEL_WIDTH);

        Img windowAnchor = new Img();

        // רינדור ראשון - עוד אין frame, אז currentContentSize נופלת חזרה
        // לגודל התחלתי קבוע. זה גם מה שפותח את החלון בפועל (show()).
        renderFrame(session, sceneView, windowAnchor);

        javax.swing.SwingUtilities.invokeLater(() -> {
            windowAnchor.onClick((pixelX, pixelY) -> {
                BoardLayout layout = computeLayout(
                        currentContentSize(windowAnchor), session[0].board.width(), session[0].board.height());
                int boardX = pixelX - layout.offsetX();
                int boardY = pixelY - layout.offsetY();
                if (boardX < 0 || boardX >= layout.boardPixelSize()
                        || boardY < 0 || boardY >= layout.boardPixelSize()) {
                    return; // קליק מחוץ ללוח - בפאנל, או בשוליים הריקים סביב הלוח הממורכז
                }
                if (session[0].engine.isGameOver()) {
                    Rectangle restartButton = sceneView.restartButtonBounds();
                    if (restartButton.contains(boardX, boardY)) {
                        session[0] = new GameSession();
                        renderFrame(session, sceneView, windowAnchor);
                    }
                    return;
                }
                session[0].controller.click(boardX, boardY, layout.cellSize(), layout.cellSize());
            });
            windowAnchor.onRightClick((pixelX, pixelY) -> {
                BoardLayout layout = computeLayout(
                        currentContentSize(windowAnchor), session[0].board.width(), session[0].board.height());
                int boardX = pixelX - layout.offsetX();
                int boardY = pixelY - layout.offsetY();
                if (boardX < 0 || boardX >= layout.boardPixelSize()
                        || boardY < 0 || boardY >= layout.boardPixelSize()) {
                    return;
                }
                if (session[0].engine.isGameOver()) {
                    return;
                }
                session[0].controller.rightClick(boardX, boardY, layout.cellSize(), layout.cellSize());
            });
        });

        long[] previousTimeNanos = { System.nanoTime() };
        Timer timer = new Timer(16, e -> {
            long now = System.nanoTime();
            long elapsedMillis = (now - previousTimeNanos[0]) / 1_000_000;
            previousTimeNanos[0] = now;

            session[0].engine.handleWait(elapsedMillis);
            renderFrame(session, sceneView, windowAnchor);
        });
        timer.start();
    }

    private static void renderFrame(GameSession[] session, GameSceneView sceneView, Img windowAnchor) {
        Dimension content = currentContentSize(windowAnchor);
        BoardLayout layout = computeLayout(content, session[0].board.width(), session[0].board.height());
        GameSnapshot snapshot = session[0].currentSnapshot(layout.cellSize());
        sceneView.render(snapshot, content.width, content.height,
                layout.boardPixelSize(), layout.offsetX(), layout.offsetY());
    }
}
