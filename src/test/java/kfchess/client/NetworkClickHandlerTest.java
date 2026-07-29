package kfchess.client;

import kfchess.input.BoardMapper;
import kfchess.model.Position;
import kfchess.view.BoardView;
import kfchess.view.PlaySceneView;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// client=null in most of these tests on purpose: resolvePosition() never touches the client (that's
// why it was extracted - pure, no network), and the handle() tests here only exercise the branches
// that return *before* the client is used (a click outside the board, or a click during gameOver
// that misses the Restart button), so no NPE can occur.
// sceneView does have to be real, because handle() asks it for restartButtonBounds() on every
// gameOver click even when the click ultimately misses. Constructing a PlaySceneView touches no
// files - the constructor only stores a path string; only render() would read one, and no test
// here calls render().
class NetworkClickHandlerTest {

    private static final BoardLayout LAYOUT = new BoardLayout(50, 400, 240, 0);

    private final PlaySceneView sceneView = new PlaySceneView(new BoardView("unused"), 240);
    private final NetworkClickHandler handler = new NetworkClickHandler(null, new BoardMapper(), sceneView);

    @Test
    void resolvePosition_clickInsideBoard_returnsCorrectPosition() {
        // Pixel (240+75, 125) with cell size 50 -> row = 125/50 = 2, col = 75/50 = 1
        Optional<Position> resolved = handler.resolvePosition(240 + 75, 125, LAYOUT);
        assertEquals(Optional.of(new Position(2, 1)), resolved);
    }

    @Test
    void resolvePosition_clickAtTopLeftCorner_returnsOrigin() {
        assertEquals(Optional.of(new Position(0, 0)), handler.resolvePosition(240, 0, LAYOUT));
    }

    @Test
    void resolvePosition_clickInSidePanel_returnsEmpty() {
        // x=100 is left of offsetX=240 - that's inside White's panel, not on the board
        assertTrue(handler.resolvePosition(100, 100, LAYOUT).isEmpty());
    }

    @Test
    void resolvePosition_clickPastRightEdgeOfBoard_returnsEmpty() {
        // offsetX(240) + boardPixelSize(400) = 640 - anything beyond that is off the board
        assertTrue(handler.resolvePosition(640, 100, LAYOUT).isEmpty());
    }

    @Test
    void resolvePosition_clickAboveBoard_returnsEmpty() {
        assertTrue(handler.resolvePosition(300, -1, LAYOUT).isEmpty());
    }

    @Test
    void resolvePosition_clickBelowBoard_returnsEmpty() {
        assertTrue(handler.resolvePosition(300, 400, LAYOUT).isEmpty());
    }

    @Test
    void handle_gameOverAndClickMissesRestartButton_doesNothingAndDoesNotTouchClient() {
        // (300,100) becomes boardX=60, boardY=100 (offsetX=240), which is outside the
        // button's vertical range. client=null is the assertion here: if handle() did
        // hit the button and call sendRestart(), this would fail with an NPE.
        handler.handle(300, 100, LAYOUT, true, false);
    }

    @Test
    void handle_gameOverAndClickHitsRestartButton_sendsRestartToServer() throws URISyntaxException {
        // A separate handler with a real RecordingPlayClient: here we do expect
        // sendRestart() to be called, so we need a spy that records it.
        RecordingPlayClient client = new RecordingPlayClient();
        NetworkClickHandler handlerWithClient = new NetworkClickHandler(client, new BoardMapper(), sceneView);

        // The pixel is derived from the actual restartButtonBounds() rather than a magic
        // number, so the test doesn't break if PlaySceneView's internal button size changes.
        Rectangle button = sceneView.restartButtonBounds();
        int pixelX = LAYOUT.offsetX() + button.x + button.width / 2;
        int pixelY = LAYOUT.offsetY() + button.y + button.height / 2;

        handlerWithClient.handle(pixelX, pixelY, LAYOUT, true, false);

        assertTrue(client.restartSent);
    }

    @Test
    void handle_clickOutsideBoardWhileGameStillRunning_doesNothingAndDoesNotTouchClient() {
        // client=null again: an empty resolvePosition must prevent the client being touched
        handler.handle(100, 100, LAYOUT, false, false);
    }
}
