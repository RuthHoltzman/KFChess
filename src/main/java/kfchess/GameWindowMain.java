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
import kfchess.view.BoardGeometry;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.Img;

import javax.swing.Timer;
import java.awt.Rectangle;
import java.util.List;
import java.util.Scanner;

public class GameWindowMain {

    private static final int CELL_SIZE = 100;
    // רוחב פאנל הניקוד/המהלכים של כל שחקן, בפיקסלים, בקצוות הלוח.
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

    /**
     * כל המצב המשתנה של "משחק נוכחי" (הלוח, המנוע, ה-controller, ומעקב
     * מצבי התצוגה בתוך ה-SnapshotFactory) מרוכז באובייקט אחד - כדי
     * שאתחול-מחדש (Restart) יוכל להחליף את כולם ביחד, באטומיות אחת,
     * במקום לפזר כמה משתנים mutable נפרדים שקל לשכוח לאפס אחד מהם
     * (למשל: לאפס את הלוח אבל לשכוח לאפס את ה-SnapshotFactory, וכך
     * להשאיר "זיכרון" ויזואלי של כלים מהמשחק הקודם).
     * <p>
     * גודל הלוח קבוע (8x8 בכל משחק), ולכן geometry/boardView/sceneView
     * *לא* חלק מהאובייקט הזה - הם נוצרים פעם אחת ב-main ונשארים זהים
     * גם אחרי Restart.
     */
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
            this.controller = new GameController(engine, new BoardMapper(CELL_SIZE));
            this.snapshotFactory = new SnapshotFactory(CELL_SIZE, CELL_SIZE);
        }

        GameSnapshot currentSnapshot() {
            Position selected = engine.selectedPosition().orElse(null);
            List<Position> legalMoves = selected == null
                    ? List.<Position>of()
                    : engine.legalMovesFrom(selected);
            String winner = engine.winner()
                    .map(color -> color == PieceColor.WHITE ? "White" : "Black")
                    .orElse(null);

            return snapshotFactory.createSnapshot(
                    board,
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

    public static void main(String[] args) {
        // עטוף במערך בגודל 1 כדי שאפשר יהיה "להחליף" משחק שלם (Restart)
        // מתוך ה-lambda-ים של onClick/Timer למטה - משתנה local רגיל לא
        // יכול להיות מוקצה-מחדש בתוך lambda, אבל האיבר של מערך יכול.
        GameSession[] session = { new GameSession() };

        String boardPath = "src/main/resources/board.png";
        Img boardImgForSizing = new Img().read(boardPath); // רק כדי לקרוא מידות, חד-פעמי
        int boardWidthPx = boardImgForSizing.get().getWidth();
        int boardHeightPx = boardImgForSizing.get().getHeight();
        BoardGeometry geometry = new BoardGeometry(
                boardWidthPx, boardHeightPx, session[0].board.height(), session[0].board.width());
        BoardView boardView = new BoardView(boardPath, geometry);
        GameSceneView sceneView = new GameSceneView(boardView, boardWidthPx, boardHeightPx, SIDE_PANEL_WIDTH);

        // רינדור ראשון - פותח את החלון ומעלה את ה-EDT
        sceneView.render(session[0].currentSnapshot());

        // רישום יחיד של מאזיני קליק (שמאל = תזוזה/Restart, ימין = קפיצה).
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
                if (session[0].engine.isGameOver()) {
                    // המשחק נגמר: הקליק היחיד שרלוונטי הוא על כפתור ה-Restart
                    // המצויר על-גבי הלוח (ר' GameSceneView.drawGameOverOverlay).
                    Rectangle restartButton = sceneView.restartButtonBounds();
                    if (restartButton.contains(boardX, pixelY)) {
                        session[0] = new GameSession();
                        sceneView.render(session[0].currentSnapshot());
                    }
                    return;
                }
                session[0].controller.click(boardX, pixelY);
            });
            boardImgForSizing.onRightClick((pixelX, pixelY) -> {
                int boardX = pixelX - sceneView.boardOffsetX();
                if (boardX < 0 || boardX >= boardWidthPx) {
                    return;
                }
                if (session[0].engine.isGameOver()) {
                    return;
                }
                session[0].controller.rightClick(boardX, pixelY);
            });
        });

        long[] previousTimeNanos = { System.nanoTime() };
        Timer timer = new Timer(16, e -> {
            long now = System.nanoTime();
            long elapsedMillis = (now - previousTimeNanos[0]) / 1_000_000;
            previousTimeNanos[0] = now;

            session[0].engine.handleWait(elapsedMillis);
            sceneView.render(session[0].currentSnapshot());
        });
        timer.start();
    }
}
