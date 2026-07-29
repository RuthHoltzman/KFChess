package kfchess.client;


import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test double for GameClient: overrides the send methods to just record what was called, without
 * touching the network. The real send() would throw NotYetConnectedException, since these tests
 * never connect to a server. Same approach as the existing FakeWebSocket stub.
 */
class RecordingGameClient extends GameClient {

    boolean restartSent = false;
    boolean clickSent = false;
    boolean jumpSent = false;

    RecordingGameClient() throws URISyntaxException {
        super(new URI("ws://localhost:1"));
    }

    @Override
    public void sendRestart() {
        restartSent = true;
    }

    @Override
    public void sendClick(int row, int col) {
        clickSent = true;
    }

    @Override
    public void sendJump(int row, int col) {
        jumpSent = true;
    }
}
