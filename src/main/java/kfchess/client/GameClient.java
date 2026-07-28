package kfchess.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kfchess.logging.FileLogger;
import kfchess.protocol.ClientCommand;
import kfchess.protocol.ClientCommandType;
import kfchess.model.ClientRole;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;


/** WebSocket client: connects to the server, sends CLICK/JUMP/RESTART, and pushes incoming messages to a listener. */
public class GameClient extends WebSocketClient {

    private final Gson gson = new Gson();

    // Notified from onMessage() on the network thread - must hop onto the EDT before touching Swing.
    private volatile Consumer<String> messageListener;

    private volatile String assignedGameId;
    private volatile ClientRole assignedRole;
    private volatile String matchmakingTimeoutMessage;
    private final FileLogger fileLogger = new FileLogger("client");

    public GameClient(URI serverUri) {
        super(serverUri);
    }

    /** Called once the WebSocket handshake succeeds. */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("connected to " + getURI());
        fileLogger.log("Connected to " + getURI());
    }

    /** Called for every message from the server: updates internal state, then notifies the listener. */
    @Override
    public void onMessage(String message) {
        if (IncomingMessageSummary.isRoleAssigned(message)) {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            assignedGameId = json.get("gameId").getAsString();
            assignedRole = ClientRole.valueOf(json.get("role").getAsString());
        }
        if (IncomingMessageSummary.isMatchmakingTimeout(message)) {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            matchmakingTimeoutMessage = json.get("message").getAsString();
        }
        if (!IncomingMessageSummary.isSnapshot(message)) {
            String summary = IncomingMessageSummary.describe(message);
            System.out.println(summary);
            fileLogger.log("Received: " + summary);
        }

        Consumer<String> listener = messageListener;
        if (listener != null) {
            listener.accept(message);
        }
    }

    /** Registers the callback invoked for every incoming message (see the field doc above). */
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }


    /** The gameId the server assigned this connection, or null before ROLE_ASSIGNED arrives. */
    public String assignedGameId() {
        return assignedGameId;
    }

    /** The role (WHITE/BLACK/SPECTATOR) the server assigned this connection, or null before ROLE_ASSIGNED arrives. */
    public ClientRole assignedRole() {
        return assignedRole;
    }

    /** The MATCHMAKING_TIMEOUT text, or null if no such message has arrived. */
    public String matchmakingTimeoutMessage() {
        return matchmakingTimeoutMessage;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("disconnected: " + reason);
        fileLogger.log("Disconnected: code=" + code + ", reason=" + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("client error: " + ex.getMessage());
        fileLogger.log("ERROR: " + ex.getMessage());
    }

    /** Sends a CLICK command for the given board position. */
    public void sendClick(int row, int col) {
        fileLogger.log("Sending CLICK row=" + row + " col=" + col);
        send(gson.toJson(new ClientCommand(ClientCommandType.CLICK, row, col)));
    }

    /** Sends a JUMP command for the given board position. */
    public void sendJump(int row, int col) {
        fileLogger.log("Sending JUMP row=" + row + " col=" + col);
        send(gson.toJson(new ClientCommand(ClientCommandType.JUMP, row, col)));
    }

    /** Asks the server to restart the game (both sides must send this before it actually resets). */
    public void sendRestart() {
        fileLogger.log("Sending RESTART");
        send(gson.toJson(new ClientCommand(ClientCommandType.RESTART, 0, 0)));
    }
}
