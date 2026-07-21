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
import kfchess.view.layout.BoardLayoutCalculator;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import java.util.Scanner;

public class GameWindowMain {

    private static final int INITIAL_CELL_SIZE = 100;
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

    // חישוב הגיאומטריה (BoardLayout/computeLayout/currentContentSize) הוצא
    // ל-kfchess.view.layout.BoardLayoutCalculator - משותף עם NetworkGameWindowMain,
    // כדי לא לשכפל אותו שם. ר' התיעוד במחלקה עצמה.

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
                BoardLayout layout = BoardLayoutCalculator.computeLayout(
                        BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE),
                        session[0].board.width(), session[0].board.height(), SIDE_PANEL_WIDTH);
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
                BoardLayout layout = BoardLayoutCalculator.computeLayout(
                        BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE),
                        session[0].board.width(), session[0].board.height(), SIDE_PANEL_WIDTH);
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
        Dimension content = BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE);
        BoardLayout layout = BoardLayoutCalculator.computeLayout(
                content, session[0].board.width(), session[0].board.height(), SIDE_PANEL_WIDTH);
        GameSnapshot snapshot = session[0].currentSnapshot(layout.cellSize());
        sceneView.render(snapshot, content.width, content.height,
                layout.boardPixelSize(), layout.offsetX(), layout.offsetY());
    }
}
