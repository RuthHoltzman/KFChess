package kfchess.io;

import kfchess.engine.GameEngine;
import kfchess.input.BoardMapper;
import kfchess.input.Controller.Command;
import kfchess.view.Renderer;
/**
 * "המנצח" הראשי של האפליקציה: היחיד שיודע *מאיפה* מגיע הקלט (Scanner
 * על System.in). היא מתרגמת כל שורה לפקודה דרך kfchess.input.Controller,
 * ומפעילה את GameEngine בהתאם. השכבה הזו לא מכילה שום חוק משחק -
 * רק "חיווט" (wiring) בין הרכיבים.
 */
public class Controller {

    private final BoardMapper boardMapper;
    private final Renderer renderer;

    public Controller(BoardMapper boardMapper, Renderer renderer) {
        this.boardMapper = boardMapper;
        this.renderer = renderer;
    }

    public void execute(Command command, GameEngine engine) {
        switch (command.type()) {
            case CLICK:
                handleClick(command, engine);
                break;
            case WAIT:
                engine.handleWait(command.milliseconds());
                break;
            case JUMP:
                engine.handleJump(boardMapper.pixelToPosition(command.x(), command.y()));
                break;
            case PRINT_BOARD:
                renderer.render(engine.board());
                break;
            case UNKNOWN:
            default:
                // התעלמות משורה ריקה או לא מזוהה
                break;
        }
    }

    private void handleClick(Command command, GameEngine engine) {
        engine.handleClick(boardMapper.pixelToPosition(command.x(), command.y()));
    }
}