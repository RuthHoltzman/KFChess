package kfchess.client;

import kfchess.input.BoardMapper;
import kfchess.model.Position;
import kfchess.view.GameSceneView;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import java.awt.Rectangle;
import java.util.Optional;

/** The client's input controller: turns mouse clicks into CLICK/JUMP/RESTART commands sent to the server. */
public class NetworkClickHandler {

    private final GameClient client;
    private final BoardMapper boardMapper;
    private final GameSceneView sceneView;

    public NetworkClickHandler(GameClient client, BoardMapper boardMapper, GameSceneView sceneView) {
        this.client = client;
        this.boardMapper = boardMapper;
        this.sceneView = sceneView;
    }

    /** Converts an absolute pixel to a board position, or empty if the click fell outside the board. */
    public Optional<Position> resolvePosition(int pixelX, int pixelY, BoardLayout layout) {
        int boardX = pixelX - layout.offsetX();
        int boardY = pixelY - layout.offsetY();
        if (boardX < 0 || boardX >= layout.boardPixelSize() || boardY < 0 || boardY >= layout.boardPixelSize()) {
            return Optional.empty();
        }
        return Optional.of(boardMapper.pixelToPosition(boardX, boardY, layout.cellSize(), layout.cellSize()));
    }


    /** Handles a click/right-click: routes to the Restart button when the game is over, else sends CLICK/JUMP. */
    public void handle(int pixelX, int pixelY, BoardLayout layout, boolean gameOver, boolean isJump) {
        if (gameOver) {
            handleRestartClick(pixelX, pixelY, layout);
            return;
        }
        resolvePosition(pixelX, pixelY, layout).ifPresent(position -> {
            if (isJump) {
                client.sendJump(position.row(), position.col());
            } else {
                client.sendClick(position.row(), position.col());
            }
        });
    }


    /** Sends RESTART if the click landed on the Restart button drawn over the game-over overlay. */
    private void handleRestartClick(int pixelX, int pixelY, BoardLayout layout) {
        int boardX = pixelX - layout.offsetX();
        int boardY = pixelY - layout.offsetY();
        Rectangle button = sceneView.restartButtonBounds();
        if (button.contains(boardX, boardY)) {
            client.sendRestart();
        }
    }
}
