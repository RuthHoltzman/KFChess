package kfchess.input;

import kfchess.engine.GameEngine;

public class GameController {

    private final GameEngine engine;
    private final BoardMapper boardMapper;

    public GameController(GameEngine engine, BoardMapper boardMapper) {
        this.engine = engine;
        this.boardMapper = boardMapper;
    }

    // cellSizeInPixels עכשיו פרמטר (לא קבוע) - הקוראת (GameWindowMain) יודעת
    // בכל רגע מהו גודל התא הנוכחי (הוא תלוי בגודל החלון), ומעבירה אותו
    // הלאה בכל קליק - כך אין שום "מספר קבוע ישן" שיכול להתיישן.
    public void click(int pixelX, int pixelY, int cellSizeInPixels) {
        engine.handleClick(boardMapper.pixelToPosition(pixelX, pixelY, cellSizeInPixels));
    }

    public void rightClick(int pixelX, int pixelY, int cellSizeInPixels) {
        engine.handleJump(boardMapper.pixelToPosition(pixelX, pixelY, cellSizeInPixels));
    }
}
