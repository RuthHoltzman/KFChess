package kfchess.net;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * לקוח WebSocket מינימלי - שלב ראשון של "צד לקוח", עוד בלי חיבור ל-GUI
 * (ר' PROGRESS.md): מטרתו רק להוכיח שהתקשורת מול GameServer עובדת -
 * מתחבר, שולח CLICK/JUMP, ומדפיס תקציר קריא של מה שהתקבל (לא JSON גולמי,
 * כדי לא להציף את המסוף כמו בבדיקה הידנית מהדפדפן - ר' IncomingMessageSummary).
 */
public class GameClient extends WebSocketClient {

    private final Gson gson = new Gson();

    public GameClient(URI serverUri) {
        super(serverUri);
    }

    // נקרא ע"י הספרייה ברגע שההתחברות הצליחה (handshake הושלם) - כאן רק מודיעים בקונסול.
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("connected to " + getURI());
    }

    // נקרא לכל הודעה נכנסת מהשרת (ROLE_ASSIGNED/SNAPSHOT/ERROR) - מתרגם לשורה קריאה אחת ומדפיס.
    @Override
    public void onMessage(String message) {
        System.out.println(IncomingMessageSummary.describe(message));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("disconnected: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("client error: " + ex.getMessage());
    }

    // בונה ClientCommand מסוג CLICK וממיר ל-JSON לפני שליחה - אותו DTO שהשרת מפענח, הפעם בכיוון ההפוך.
    public void sendClick(int row, int col) {
        send(gson.toJson(new ClientCommand(ClientCommandType.CLICK, row, col)));
    }

    // כנ"ל, עבור JUMP.
    public void sendJump(int row, int col) {
        send(gson.toJson(new ClientCommand(ClientCommandType.JUMP, row, col)));
    }
}
