package kfchess.input;

import kfchess.engine.GameEngine;

public class GameController {

    private final GameEngine engine;
    private final BoardMapper boardMapper;

    public GameController(GameEngine engine, BoardMapper boardMapper) {
        this.engine = engine;
        this.boardMapper = boardMapper;
    }

    // cellWidth/cellHeight עכשיו שני פרמטרים נפרדים (לא cellSize יחיד) -
    // הקוראת (GameWindowMain) יודעת בכל רגע מהם, ומעבירה אותם הלאה בכל
    // קליק - כדי שמיפוי ציר ה-Y ישתמש בגובה התא האמיתי, לא ברוחבו.
    public void click(int pixelX, int pixelY, int cellWidth, int cellHeight) {
        engine.handleClick(boardMapper.pixelToPosition(pixelX, pixelY, cellWidth, cellHeight));
    }

    public void rightClick(int pixelX, int pixelY, int cellWidth, int cellHeight) {
        engine.handleJump(boardMapper.pixelToPosition(pixelX, pixelY, cellWidth, cellHeight));
    }
}
