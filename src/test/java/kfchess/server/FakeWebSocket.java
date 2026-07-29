package kfchess.server;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * A fake WebSocket for tests only. PlaySession uses a WebSocket purely as an identity key in the
 * connections map and the pending-command queue, so none of these methods are ever really called -
 * they are all minimal stubs with no behavior.
 */
class FakeWebSocket implements WebSocket {

    @Override
    public void close(int code, String message) {
    }

    @Override
    public void close(int code) {
    }

    @Override
    public void close() {
    }

    @Override
    public void closeConnection(int code, String message) {
    }

    @Override
    public void send(String text) {
    }

    @Override
    public void send(ByteBuffer bytes) {
    }

    @Override
    public void send(byte[] bytes) {
    }

    @Override
    public void sendFrame(Framedata framedata) {
    }

    @Override
    public void sendFrame(Collection<Framedata> frames) {
    }

    @Override
    public void sendPing() {
    }

    @Override
    public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {
    }

    @Override
    public boolean hasBufferedData() {
        return false;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isClosing() {
        return false;
    }

    @Override
    public boolean isFlushAndClose() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public Draft getDraft() {
        return null;
    }

    @Override
    public ReadyState getReadyState() {
        return ReadyState.OPEN;
    }

    @Override
    public String getResourceDescriptor() {
        return "/test";
    }

    @Override
    public <T> void setAttachment(T attachment) {
    }

    @Override
    public <T> T getAttachment() {
        return null;
    }

    @Override
    public boolean hasSSLSupport() {
        return false;
    }

    @Override
    public SSLSession getSSLSession() {
        return null;
    }

    @Override
    public IProtocol getProtocol() {
        return null;
    }
}
