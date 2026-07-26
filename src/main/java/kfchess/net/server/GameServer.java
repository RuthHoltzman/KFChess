package kfchess.net.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import kfchess.account.AccountRepository;
import kfchess.account.SqliteAccountRepository;
import kfchess.net.ClientCommand;
import kfchess.net.ClientRole;
import kfchess.net.ErrorMessage;
import kfchess.net.RoleAssignedMessage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * נקודת הכניסה לרשת: מחזיק משחק (GameSession) לכל gameId, ומריץ thread
 * יחיד ("single writer") שהוא היחיד שקורא ל-tick על כל משחק ומשדר את
 * מצבו - כל מה ש-onOpen/onMessage/onClose (שרצים על threads הרשת של
 * הספרייה) עושים הוא לתייק פקודות/לעדכן מיפויי חיבורים, לא לגעת
 * ב-GameEngine ישירות (ר' התיעוד ב-GameSession).
 */
public class GameServer extends WebSocketServer {

    private static final long TICK_INTERVAL_MILLIS = 33; // ~30 עדכונים בשנייה

    private final Gson gson = new Gson();
    // repository אחד, משותף לכל ה-GameSession-ים (וגם ל-LoginScreenMain בצד
    // הלקוח) - אותו קובץ kfchess.db בדיוק, כי אלה אותם חשבונות עצמם.
    private final AccountRepository accountRepository =
            new SqliteAccountRepository(SqliteAccountRepository.DEFAULT_DB_FILE);
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> gameIdByConnection = new ConcurrentHashMap<>();
    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();
    private long lastTickNanos = System.nanoTime();

    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    // נקרא פעם אחת אחרי שהשקע נפתח בהצלחה: כאן מתחילה לולאת הטיק, על thread נפרד מזה של הרשת.
    @Override
    public void onStart() {
        lastTickNanos = System.nanoTime();
        tickExecutor.scheduleAtFixedRate(this::tickAllSessions, 0, TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        System.out.println("GameServer started on port " + getPort());
    }

    // חיבור חדש: קובע/יוצר את המשחק לפי הנתיב, קובע תפקיד (לבן/שחור/צופה)
    // + משייך את ה-username אם הגיע אחד (ר' UsernameResolver - שלב 4 Part B,
    // דרוש לעדכון ELO בסוף המשחק), ומודיע ללקוח מיד.
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String gameId = GameIdResolver.resolve(handshake.getResourceDescriptor());
        String username = UsernameResolver.resolve(handshake.getResourceDescriptor()).orElse(null);
        GameSession session = sessions.computeIfAbsent(gameId, id -> new GameSession(accountRepository));
        gameIdByConnection.put(conn, gameId);
        ClientRole role = session.assignRole(conn, username);
        conn.send(gson.toJson(new RoleAssignedMessage(role.name(), gameId)));
    }

    // ניתוק: מעביר ל-GameSession.handleDisconnect (במקום removeConnection הישנה)
    // כדי ש-WHITE/BLACK באמצע משחק פעיל יקבלו "חלון חסד" לחיבור מחדש
    // (שלב 5, auto-resign) במקום להפסיד/להיתקע מיד - ר' תיעוד handleDisconnect.
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String gameId = gameIdByConnection.remove(conn);
        GameSession session = gameId == null ? null : sessions.get(gameId);
        if (session != null) {
            session.handleDisconnect(conn);
        }
    }

    // הודעה נכנסת: רק מפענחת ומתייקת (enqueueCommand) - הביצוע בפועל קורה ב-tick, לא כאן.
    @Override
    public void onMessage(WebSocket conn, String message) {
        GameSession session = sessions.get(gameIdByConnection.get(conn));
        if (session == null) {
            return;
        }
        try {
            ClientCommand command = gson.fromJson(message, ClientCommand.class);
            if (command == null || !command.isValid()) {
                conn.send(gson.toJson(new ErrorMessage("invalid command: " + message)));
                return;
            }
            session.enqueueCommand(conn, command);
        } catch (JsonSyntaxException malformedJson) {
            conn.send(gson.toJson(new ErrorMessage("malformed JSON: " + malformedJson.getMessage())));
        }
    }

    // שגיאת רשת - רק לוג, לא מפילה את השרת (חיבור בודד עשוי להיסגר בעקבותיה, זה מטופל ע"י onClose בנפרד).
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("GameServer error: " + ex.getMessage());
    }

    // רץ אך ורק על thread הטיק: מקדם את כל המשחקים הפעילים לפי הזמן שעבר, ואז משדר לכל אחד את מצבו.
    private void tickAllSessions() {
        long now = System.nanoTime();
        long elapsedMillis = (now - lastTickNanos) / 1_000_000;
        lastTickNanos = now;

        for (GameSession session : sessions.values()) {
            session.tick(elapsedMillis);
            broadcast(session);
        }
    }

    // שולח לכל חיבור במשחק snapshot מותאם לתפקיד שלו; חיבור שכבר נסגר בשקט מדולג (לא מפיל את שאר השידור).
    private void broadcast(GameSession session) {
        session.connections().forEach((conn, role) -> {
            try {
                conn.send(gson.toJson(session.snapshotFor(role)));
            } catch (RuntimeException sendFailed) {
                // חיבור שכבר לא תקין (למשל WebsocketNotConnectedException) - מתעלמים, ה-onClose כבר בדרך.
            }
        });
    }
}
