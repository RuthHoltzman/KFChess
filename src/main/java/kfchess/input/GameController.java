package kfchess.input;

import kfchess.engine.GameEngine;

public class GameController {

    private final GameEngine engine;
    private final BoardMapper boardMapper;

    public GameController(GameEngine engine, BoardMapper boardMapper) {
        this.engine = engine;
        this.boardMapper = boardMapper;
    }

    public void click(int pixelX, int pixelY) {
        engine.handleClick(boardMapper.pixelToPosition(pixelX, pixelY));
    }
    public void rightClick(int pixelX, int pixelY) {
    engine.handleJump(boardMapper.pixelToPosition(pixelX, pixelY));
}
}