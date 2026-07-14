package kfchess;

import kfchess.engine.GameEngine;
import kfchess.input.BoardMapper;
import kfchess.input.Controller.Command;
import kfchess.io.BoardPrinter;

/**
 * מבצעת פקודה בודדת (שכבר פוענחה) על גבי GameEngine.
 * זהו קלאס ה-wiring: הוא "יודע" איך לתרגם CLICK/WAIT/JUMP/PRINT_BOARD
 * לקריאות בפועל ל-engine/boardMapper/boardPrinter - בלי לפרש טקסט
 * גולמי בעצמו (זה תפקידו של Controller/CommandParser) ובלי לרוץ בלולאה
 * בעצמו (זה תפקידו של ConsoleRunner).
 */
public class CommandRunner {

    private final GameEngine engine;
    private final BoardMapper boardMapper;
    private final BoardPrinter boardPrinter;

    public CommandRunner(GameEngine engine, BoardMapper boardMapper, BoardPrinter boardPrinter) {
        this.engine = engine;
        this.boardMapper = boardMapper;
        this.boardPrinter = boardPrinter;
    }

    public void run(Command command) {
        switch (command.type()) {
            case CLICK:
                engine.handleClick(boardMapper.pixelToPosition(command.x(), command.y()));
                break;
            case WAIT:
                engine.handleWait(command.milliseconds());
                break;
            case JUMP:
                engine.handleJump(boardMapper.pixelToPosition(command.x(), command.y()));
                break;
            case PRINT_BOARD:
                boardPrinter.print(engine.board());
                break;
            case UNKNOWN:
            default:
                // התעלמות משורה ריקה או לא מזוהה
                break;
        }
    }
}