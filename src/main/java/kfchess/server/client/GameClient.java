package kfchess.server.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kfchess.logging.FileLogger;
import kfchess.server.ClientCommand;
import kfchess.server.ClientCommandType;
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
    // נחתך מתוך הודעת ROLE_ASSIGNED הראשונה (ר' onMessage) - שלב 6:
    // "Create room" - השרת הוא זה שממציא את ה-gameId, הלקוח לא יודע
    // אותו מראש בכלל, אז חייבים לחלץ אותו מהתשובה הראשונה כדי להציג
    // אותו בכותרת חלון המשחק (ר' NetworkGameWindowMain, Img.setTitle).
    private volatile String assignedGameId;
    // תיקון "Play" לפי המפרט המדויק (ELO ±100 / timeout של דקה) - נחתך
    // מתוך הודעת MATCHMAKING_TIMEOUT אם/כשמגיעה (ר' onMessage). null כל
    // עוד לא הגיעה כזו הודעה - NetworkGameWindowMain בודק את זה בכל טיק
    // (בדיוק כמו assignedGameId/latestMessage, אותו דפוס polling).
    private volatile String matchmakingTimeoutMessage;
    // שלב 6 חלק 2 (בקשת המנחה): לוג טכני/תפעולי לקובץ טקסט - ר' תיעוד
    // FileLogger וגם GameServer.fileLogger (אותו רעיון, בצד הלקוח הפעם).
    // אחד לכל GameClient - כלומר אחד לכל ניסיון חיבור (ר' HomeScreenMain.connect,
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
        latestMessage = message;
        if (IncomingMessageSummary.isRoleAssigned(message)) {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            assignedGameId = json.get("gameId").getAsString();
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
    }

    // ה-gameId בפועל שהשרת הקצה לחיבור הזה (ר' RoleAssignedMessage), או
    // null אם ROLE_ASSIGNED עוד לא הגיעה. volatile כבר מבטיח קריאה בטוחה
    // בין threads (בדיוק כמו latestMessage למעלה) - HomeScreenMain קורא
    // לזה מ-thread רקע אחרי connectBlocking (ר' waitForAssignedGameId).
    public String assignedGameId() {
        return assignedGameId;
    }

    // הטקסט מתוך MATCHMAKING_TIMEOUT (ר' MatchmakingTimeoutMessage), או
    // null אם עוד לא התקבלה כזו הודעה. NetworkGameWindowMain בודק את
    // זה בכל טיק (בדיוק כמו assignedGameId למעלה) כדי להציג popup ולסגור
    // את החלון ברגע שהיא מגיעה.
    public String matchmakingTimeoutMessage() {
        return matchmakingTimeoutMessage;
    }

    // JSON הגולמי של ההודעה האחרונה שהתקבלה (או null אם עוד לא התקבל כלום) -
    // צריך ל-NetworkGameWindowMain כדי "לסקור" (polling) מ-Timer של Swing
    // במקום callback מ-thread הרשת; volatile כבר מבטיח קריאה בטוחה בין
    // threads (ר' onMessage), אז אין צורך בסנכרון נוסף כאן.
    public String latestMessage() {
        return latestMessage;
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
