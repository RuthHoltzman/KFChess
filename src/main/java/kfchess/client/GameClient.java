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


public class GameClient extends WebSocketClient {

    private final Gson gson = new Gson();

    // Called from onMessage() for every incoming message - runs on the network
    // thread, so the listener itself must hop onto the EDT (SwingUtilities.invokeLater)
    // before touching any Swing state. Replaces the old polling-based latestMessage()
    // so NetworkGameWindow can repaint only when there's actually something new,
    // instead of redrawing the same frame on a fixed timer.
    private volatile Consumer<String> messageListener;

    private volatile String assignedGameId;
    private volatile ClientRole assignedRole;
    // תיקון "Play" לפי המפרט המדויק (ELO ±100 / timeout של דקה) - נחתך
    // מתוך הודעת MATCHMAKING_TIMEOUT אם/כשמגיעה (ר' onMessage). null כל
    // עוד לא הגיעה כזו הודעה - NetworkGameWindow קורא את זה מתוך
    // messageListener ברגע שהודעה כזו מגיעה (ר' setMessageListener).
    private volatile String matchmakingTimeoutMessage;
    // שלב 6 חלק 2 (בקשת המנחה): לוג טכני/תפעולי לקובץ טקסט - ר' תיעוד
    // FileLogger וגם GameServer.fileLogger (אותו רעיון, בצד הלקוח הפעם).
    // אחד לכל GameClient - כלומר אחד לכל ניסיון חיבור (ר' HomeScreen.connect,
    // שיוצרת GameClient חדש בכל לחיצת Play/Room), בדיוק מה שרות ביקשה
    // ("קובץ בכל הרצה").
    private final FileLogger fileLogger = new FileLogger("client");

    public GameClient(URI serverUri) {
        super(serverUri);
    }

    // נקרא ע"י הספרייה ברגע שההתחברות הצליחה (handshake הושלם) - כאן רק מודיעים בקונסול.
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("connected to " + getURI());
        fileLogger.log("Connected to " + getURI());
    }

    // נקרא לכל הודעה נכנסת מהשרת - תמיד נשמרת (ל-status), אבל מודפסת מיד רק אם היא לא SNAPSHOT
    // (אלה מגיעות ברצף מהיר, ר' printLatestSnapshot להצגה לפי דרישה במקום הצפת מסוף).
    // אותו כלל חל על הלוג לקובץ - SNAPSHOT לא נרשמת (30 פעם בשנייה זה
    // הרבה מדי טקסט בלי תועלת), רק סוגי הודעות אחרים (ROLE_ASSIGNED/ERROR).
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
        // Fire last, so the listener already sees the up-to-date assignedRole/
        // matchmakingTimeoutMessage fields set above if it needs them.
        Consumer<String> listener = messageListener;
        if (listener != null) {
            listener.accept(message);
        }
    }

    /** Registers the callback invoked for every incoming message (see the field doc above). */
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    // ה-gameId בפועל שהשרת הקצה לחיבור הזה (ר' RoleAssignedMessage), או
    // null אם ROLE_ASSIGNED עוד לא הגיעה. volatile כבר מבטיח קריאה בטוחה
    // בין threads - HomeScreen קורא לזה מ-thread רקע אחרי connectBlocking
    // (ר' waitForAssignedGameId).
    public String assignedGameId() {
        return assignedGameId;
    }

    // התפקיד בפועל שהשרת הקצה לחיבור הזה (ר' RoleAssignedMessage), או
    // null אם ROLE_ASSIGNED עוד לא הגיעה (volatile, בלי סנכרון נוסף).
    public ClientRole assignedRole() {
        return assignedRole;
    }

    // הטקסט מתוך MATCHMAKING_TIMEOUT (ר' MatchmakingTimeoutMessage), או
    // null אם עוד לא התקבלה כזו הודעה. NetworkGameWindow קורא את זה מתוך
    // messageListener כדי להציג popup ולסגור את החלון ברגע שההודעה מגיעה.
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

    // בונה ClientCommand מסוג CLICK וממיר ל-JSON לפני שליחה - אותו DTO שהשרת מפענח, הפעם בכיוון ההפוך.
    public void sendClick(int row, int col) {
        fileLogger.log("Sending CLICK row=" + row + " col=" + col);
        send(gson.toJson(new ClientCommand(ClientCommandType.CLICK, row, col)));
    }

    // כנ"ל, עבור JUMP.
    public void sendJump(int row, int col) {
        fileLogger.log("Sending JUMP row=" + row + " col=" + col);
        send(gson.toJson(new ClientCommand(ClientCommandType.JUMP, row, col)));
    }

    // מבקש מהשרת לאתחל את הלוח (שני הצדדים צריכים לבקש - ר' GameSession.applyCommand).
    // row/col הם "דמה" (0,0) - RESTART לא צריך מיקום בכלל, ר' ClientCommand.isValid().
    public void sendRestart() {
        fileLogger.log("Sending RESTART");
        send(gson.toJson(new ClientCommand(ClientCommandType.RESTART, 0, 0)));
    }
}
