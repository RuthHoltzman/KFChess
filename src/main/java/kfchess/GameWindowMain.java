package kfchess;

import kfchess.engine.GameEngine;
import kfchess.engine.GameSnapshot;
import kfchess.engine.SnapshotFactory;
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

    // גודל תא רק לרינדור הראשון - לפני שהחלון בכלל קיים, אין עדיין
    // "גודל חלון נוכחי" לשאול. מהרינדור השני ואילך (בטיימר) כבר שואלים
    // את גודל החלון האמיתי בכל פעם מחדש - ר' currentCellWidth/Height.
    private static final int INITIAL_CELL_SIZE = 100;
    // הגנה מפני חלון שנגרר קטן מדי - חילוק שלמים יכול להתקרב ל-0.
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
            this.engine = new GameEngine(game, new RuleEngine(), new RaelTime());
            this.controller = new GameController(engine, new BoardMapper());
            this.snapshotFactory = new SnapshotFactory();
        }

        GameSnapshot currentSnapshot(int cellWidth, int cellHeight) {
            Position selected = engine.selectedPosition().orElse(null);
            List<Position> legalMoves = selected == null
                    ? List.<Position>of()
                    : engine.legalMovesFrom(selected);
            String winner = engine.winner()
                    .map(color -> color == PieceColor.WHITE ? "White" : "Black")
                    .orElse(null);

            return snapshotFactory.createSnapshot(
                    board,
                    cellWidth,
                    cellHeight,
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

    private static int clampedCellSize(int rawSizePx, int cellCount) {
        return Math.max(MIN_CELL_SIZE, rawSizePx / cellCount);
    }

    /**
     * גודל תא נוכחי, בפועל, ברגע הקריאה - שואלת ישירות את ה-Swing frame
     * (dropAnchor.contentSize()) אם הוא כבר קיים, ולא איזשהו משתנה שנשמר
     * מ-resize קודם. זה מה שמבטיח שרינדור וטיפול בקליק, אפילו אם הם
     * קורים במילישניות שונות, תמיד "רואים" בדיוק את אותו גודל - אין
     * חלון-זמן שבו אחד מהם מסתמך על ערך שכבר התיישן.
     */
    private static Dimension currentBoardRawSizePx(Img windowAnchor) {
        if (!windowAnchor.isReady()) {
            return new Dimension(
                    INITIAL_CELL_SIZE * 8 /* עמודות */, INITIAL_CELL_SIZE * 8 /* שורות */);
        }
        Dimension content = windowAnchor.contentSize();
        int rawWidth = Math.max(1, content.width - SIDE_PANEL_WIDTH * 2);
        int rawHeight = Math.max(1, content.height);
        return new Dimension(rawWidth, rawHeight);
    }

    public static void main(String[] args) {
        GameSession[] session = { new GameSession() };

        BoardView boardView = new BoardView("src/main/resources/board.png");
        GameSceneView sceneView = new GameSceneView(boardView, SIDE_PANEL_WIDTH);

        // Img לא צריך לטעון שום קובץ כדי לשמש "עוגן" לחלון - show()/onClick/
        // contentSize() נוגעים רק בשדות ה-static (frame/label) המשותפים.
        Img windowAnchor = new Img();

        // רינדור ראשון - עוד אין frame, אז currentBoardRawSizePx נופלת
        // חזרה ל-INITIAL_CELL_SIZE. זה גם מה שפותח את החלון בפועל (show()).
        Dimension initialRaw = currentBoardRawSizePx(windowAnchor);
        int initialCellWidth = clampedCellSize(initialRaw.width, session[0].board.width());
        int initialCellHeight = clampedCellSize(initialRaw.height, session[0].board.height());
        sceneView.render(
                session[0].currentSnapshot(initialCellWidth, initialCellHeight),
                initialCellWidth * session[0].board.width(),
                initialCellHeight * session[0].board.height());

        javax.swing.SwingUtilities.invokeLater(() -> {
            windowAnchor.onClick((pixelX, pixelY) -> {
                Dimension raw = currentBoardRawSizePx(windowAnchor);
                int cellWidth = clampedCellSize(raw.width, session[0].board.width());
                int boardWidthPx = cellWidth * session[0].board.width();
                int boardX = pixelX - sceneView.boardOffsetX();
                if (boardX < 0 || boardX >= boardWidthPx) {
                    return; // קליק בתוך אחד הפאנלים - לא על הלוח
                }
                if (session[0].engine.isGameOver()) {
                    Rectangle restartButton = sceneView.restartButtonBounds();
                    if (restartButton.contains(boardX, pixelY)) {
                        session[0] = new GameSession();
                        int freshCellWidth = clampedCellSize(raw.width, session[0].board.width());
                        int freshCellHeight = clampedCellSize(raw.height, session[0].board.height());
                        sceneView.render(
                                session[0].currentSnapshot(freshCellWidth, freshCellHeight),
                                freshCellWidth * session[0].board.width(),
                                freshCellHeight * session[0].board.height());
                    }
                    return;
                }
                session[0].controller.click(boardX, pixelY, cellWidth);
            });
            windowAnchor.onRightClick((pixelX, pixelY) -> {
                Dimension raw = currentBoardRawSizePx(windowAnchor);
                int cellWidth = clampedCellSize(raw.width, session[0].board.width());
                int boardWidthPx = cellWidth * session[0].board.width();
                int boardX = pixelX - sceneView.boardOffsetX();
                if (boardX < 0 || boardX >= boardWidthPx) {
                    return;
                }
                if (session[0].engine.isGameOver()) {
                    return;
                }
                session[0].controller.rightClick(boardX, pixelY, cellWidth);
            });
            // שימי לב: אין כאן onResize בכלל יותר - הוא לא נחוץ לנכונות,
            // כי הטיימר למטה כבר שואל את הגודל האמיתי בכל טיק (עד 16ms
            // מרגע גרירת שינוי הגודל). היה אפשר להוסיף אותו בחזרה רק כדי
            // "לזרז" את הרינדור הראשון אחרי resize בכמה מילישניות - אבל
            // זה שיפור קוסמטי, לא תיקון נכונות.
        });

        long[] previousTimeNanos = { System.nanoTime() };
        Timer timer = new Timer(16, e -> {
            long now = System.nanoTime();
            long elapsedMillis = (now - previousTimeNanos[0]) / 1_000_000;
            previousTimeNanos[0] = now;

            session[0].engine.handleWait(elapsedMillis);

            Dimension raw = currentBoardRawSizePx(windowAnchor);
            int cellWidth = clampedCellSize(raw.width, session[0].board.width());
            int cellHeight = clampedCellSize(raw.height, session[0].board.height());
            GameSnapshot snapshot = session[0].currentSnapshot(cellWidth, cellHeight);
            sceneView.render(snapshot, cellWidth * session[0].board.width(), cellHeight * session[0].board.height());
        });
        timer.start();
    }
}
