package kfchess;
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
        GameEngine engine = new GameEngine(game, new RuleEngine(), new RaelTime());

        CommandRunner commandRunner = new CommandRunner(
                engine, BoardMapper.withDefaultCellSize(), new BoardPrinter());
        ConsoleRunner consoleRunner = new ConsoleRunner(scanner, new Controller(), commandRunner);

        consoleRunner.run();
    }
}