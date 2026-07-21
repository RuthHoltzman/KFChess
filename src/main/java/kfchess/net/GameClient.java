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
    private volatile String latestMessage;

    public GameClient(URI serverUri) {
        super(serverUri);
    }

    // נקרא ע"י הספרייה ברגע שההתחברות הצליחה (handshake הושלם) - כאן רק מודיעים בקונסול.
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("connected to " + getURI());
    }

    // נקרא לכל הודעה נכנסת מהשרת - תמיד נשמרת (ל-status), אבל מודפסת מיד רק אם היא לא SNAPSHOT
    // (אלה מגיעות ברצף מהיר, ר' printLatestSnapshot להצגה לפי דרישה במקום הצפת מסוף).
    @Override
    public void onMessage(String message) {
        latestMessage = message;
        if (!IncomingMessageSummary.isSnapshot(message)) {
            System.out.println(IncomingMessageSummary.describe(message));
        }
    }

    // JSON הגולמי של ההודעה האחרונה שהתקבלה (או null אם עוד לא התקבל כלום) -
    // צריך ל-NetworkGameWindowMain כדי "לסקור" (polling) מ-Timer של Swing
    // במקום callback מ-thread הרשת; volatile כבר מבטיח קריאה בטוחה בין
    // threads (ר' onMessage), אז אין צורך בסנכרון נוסף כאן.
    public String latestMessage() {
        return latestMessage;
    }

    // מדפיס את התקציר של ה-snapshot האחרון שהתקבל - נקרא רק לפי דרישה (פקודת "status" ב-ClientMain).
    public void printLatestSnapshot() {
        if (latestMessage == null) {
            System.out.println("no message received yet");
            return;
        }
        System.out.println(IncomingMessageSummary.describe(latestMessage));
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
