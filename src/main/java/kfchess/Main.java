package kfchess;
import kfchess.bus.EventBus;
import kfchess.bus.GameLifecycleEvent;
import kfchess.bus.MoveLoggedEvent;
import kfchess.bus.ScoreUpdatedEvent;
import kfchess.engine.GameEngine;
import kfchess.input.BoardMapper;
import kfchess.input.Controller;
import kfchess.io.BoardParser;
import kfchess.io.BoardPrinter;
import kfchess.model.Board;
import kfchess.model.Game;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;

import java.util.Scanner;

// קישור לפרויקט בחשבון GitHub: https://github.com/RuthHoltzman/KFChess.git

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // קריאת ופענוח מצב הלוח הראשוני. אם הקלט לא תקין, ה-BoardParser
        // זורק IllegalArgumentException עם קוד השגיאה המדויק (למשל
        // "ERROR ROW_WIDTH_MISMATCH") - את זה בדיוק מדפיסים, בלי לנחש.
        Board board;
        try {
            board = new BoardParser(scanner).readBoard();
        } catch (IllegalArgumentException invalidBoard) {
            System.out.println(invalidBoard.getMessage());
            return;
        }

        Game game = new Game(board);
        EventBus bus = new EventBus();
        // מאזיני הדגמה זמניים - מוכיחים שה-bus עובד בפועל (ר' פלט [BUS] בקונסולה).
        // בשלב 2 (שרת ה-WebSocket) אלה יוחלפו/יורחבו במאזינים אמיתיים ששולחים
        // ללקוחות, ואז אפשר יהיה למחוק את השלושה האלה.
        bus.subscribe(MoveLoggedEvent.class,
                e -> System.out.println("[BUS] move: " + e.color() + " " + e.notation()));
        bus.subscribe(ScoreUpdatedEvent.class,
                e -> System.out.println("[BUS] score: " + e.color() + " = " + e.newScore()));
        bus.subscribe(GameLifecycleEvent.class,
                e -> System.out.println("[BUS] lifecycle: " + e.phase() + " winner=" + e.winner()));
        GameEngine engine = new GameEngine(game, new RuleEngine(), new RaelTime(), bus);
        CommandRunner commandRunner = new CommandRunner(
                engine, new BoardMapper(), new BoardPrinter());
        ConsoleRunner consoleRunner = new ConsoleRunner(scanner, new Controller(), commandRunner);

        consoleRunner.run();
    }
}