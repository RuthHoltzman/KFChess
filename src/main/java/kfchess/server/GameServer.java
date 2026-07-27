package kfchess.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import kfchess.account.AccountRepository;
import kfchess.account.SqliteAccountRepository;
import kfchess.logging.FileLogger;
import kfchess.protocol.ClientCommand;
import kfchess.model.ClientRole;
import kfchess.protocol.ErrorMessage;
import kfchess.protocol.MatchmakingTimeoutMessage;
import kfchess.protocol.RoleAssignedMessage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    // תיקון "Play" לפי המפרט המדויק (התגלה מאוחר, ר' "הערה חשובה - קובץ
    // ההוראות המקורי" ב-PROGRESS.md; רות בחרה לדחות אותו עד אחרי שלב 6):
    // "ELO in range of ±100... waits for 1 min... pops up a message that
    // can't find". שני הקבועים למטה - ר' resolveMatchmakingGameId/checkMatchmakingTimeout.
    private static final int ELO_MATCH_RANGE = 100;
    private static final long MATCHMAKING_TIMEOUT_MILLIS = 60_000;

    private final Gson gson = new Gson();
    // repository אחד, משותף לכל ה-GameSession-ים (וגם ל-LoginScreenMain בצד
    // הלקוח) - אותו קובץ kfchess.db בדיוק, כי אלה אותם חשבונות עצמם.
    private final AccountRepository accountRepository =
            new SqliteAccountRepository(SqliteAccountRepository.DEFAULT_DB_FILE);
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> gameIdByConnection = new ConcurrentHashMap<>();
    // gameId-ים שנוצרו *דרך Play עצמו* (ר' resolveMatchmakingGameId) - בלי
    // זה, סריקת ה-matchmaking הייתה עלולה "לגנוב" session שממתין ליריב אבל
    // נוצר דרך Create/Join (חדר פרטי/בשם) - Play חייב להתאים רק למי שגם
    // הוא/היא הגיע/ה דרך Play. ConcurrentHashMap.newKeySet() כי נקרא גם
    // מ-thread הרשת (onOpen/resolveMatchmakingGameId).
    private final Set<String> matchmakingSessionIds = ConcurrentHashMap.newKeySet();
    // gameId (מ-matchmakingSessionIds) -> רגע (System.currentTimeMillis)
    // שבו פג ה-1-דקה timeout שלו (ר' checkMatchmakingTimeout) - רק ל-
    // sessions שנוצרו *חדשים* דרך Play בלי שנמצא/ה match מיידי; מוסר
    // ברגע שנמצא match (resolveMatchmakingGameId) או שה-timeout כבר נורה.
    // זמן-קיר אמיתי בכוונה, לא שעון-המשחק המדומה (RaelTime) של GameSession -
    // GameServer ממילא לא נבדק ביחידה (דורש שרת/רשת חיים), כבר משתמש
    // ב-System.nanoTime() ישירות ב-tickAllSessions.
    private final Map<String, Long> matchmakingDeadlines = new ConcurrentHashMap<>();
    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();
    // נעילה ייעודית ל"החלטה איזה gameId להשתמש בו" - גם matchmaking
    // (ר' resolveMatchmakingGameId) וגם Create room (ר' createNewRoomGameId)
    // חולקים אותה, כי שתיהן אותה בעיה בעצם ("תמצא/י או תמציא/י gameId פנוי
    // ותשמרי אותו לפני שמישהו אחר עושה בדיוק אותו דבר"). נפרדת מהנעילות
    // הפנימיות של GameSession עצמה (assignRole וכו') כי כאן הבעיה היא ברמת
    // GameServer: איזה session בכלל נבחר, לא מה קורה בתוכו.
    private final Object sessionAllocationLock = new Object();
    // שלב 6 חלק 2 (בקשת המנחה): לוג טכני/תפעולי לקובץ טקסט - לא קשור
    // בכלל ל-moveLog (רישום מהלכי שחמט, כבר קיים) - ר' תיעוד FileLogger.
    // אחד בלבד לכל תהליך שרת (לא לכל session) - "פעילות שרת" היא
    // ברמת ה-GameServer, לא ברמת משחק בודד.
    private final FileLogger fileLogger = new FileLogger("server");
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
        fileLogger.log("GameServer started on port " + getPort());
    }

    // חיבור חדש: קובע/יוצר את המשחק לפי הנתיב, קובע תפקיד (לבן/שחור/צופה)
    // + משייך את ה-username אם הגיע אחד (ר' UsernameResolver - שלב 4 Part B,
    // דרוש לעדכון ELO בסוף המשחק), ומודיע ללקוח מיד.
    // שלב 5 חלק 2 (matchmaking, כפתור "Skip"): אם הנתיב הוא בקשת matchmaking
    // (ר' MatchmakingResolver) - ה-gameId לא נקבע מהנתיב עצמו אלא נמצא/נוצר
    // דינמית (ר' resolveMatchmakingGameId); אחרת בדיוק ההתנהגות הקיימת
    // (חדר-בשם-ספציפי, GameIdResolver). שום דבר אחר כאן לא משתנה - גם
    // matchmaking בסוף עובר דרך אותו computeIfAbsent (בטוח לקרוא לו שוב
    // גם אם resolveMatchmakingGameId כבר יצרה את ה-session - no-op).
    // שלב 6 (Create room, כפתור Create בדיאלוג Room): אם הנתיב הוא בקשת
    // "Create" (ר' CreateRoomResolver) - ה-gameId מגיע מ-createNewRoomGameId
    // (קוד קצר שהשרת ממציא, ר' RoomIdGenerator) - נבדק *לפני* matchmaking,
    // כי שני הנתיבים השמורים (_create/_play) הדדית בלעדיים.
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String path = handshake.getResourceDescriptor();
        // הוזז לפני חישוב ה-gameId (היה אחרי) - resolveMatchmakingGameId
        // צריכה את ה-username כדי לבדוק התאמת ELO (ר' תיעוד שם).
        String username = UsernameResolver.resolve(path).orElse(null);
        String gameId;
        String connectionMethod;
        if (CreateRoomResolver.isCreateRoomRequest(path)) {
            gameId = createNewRoomGameId();
            connectionMethod = "Create";
        } else if (MatchmakingResolver.isMatchmakingRequest(path)) {
            gameId = resolveMatchmakingGameId(username);
            connectionMethod = "Play";
        } else {
            gameId = GameIdResolver.resolve(path);
            connectionMethod = "Join";
        }
        GameSession session = sessions.computeIfAbsent(gameId, id -> new GameSession(accountRepository));
        gameIdByConnection.put(conn, gameId);
        ClientRole role = session.assignRole(conn, username);
        fileLogger.log("Connection opened via " + connectionMethod + ": gameId=" + gameId
                + ", role=" + role + ", username=" + (username == null ? "-" : username));
        conn.send(gson.toJson(new RoleAssignedMessage(role.name(), gameId)));
    }

    // מוצאת session קיים שממתין ליריב **עם ELO תואם** (ר' isCompatibleElo)
    // ומצטרפת אליו; אם אין כזה - יוצרת session חדש עם gameId ייחודי
    // ("match-<uuid>", לא ניתן להקליד ידנית בשדה room) ורושמת לו דדליין
    // של דקה (ר' matchmakingDeadlines/checkMatchmakingTimeout).
    // synchronized(sessionAllocationLock) חובה: בלי זה, שני חיבורים שמגיעים כמעט
    // בו-זמנית עלולים *שניהם* לסרוק ולא למצוא אף session ממתין (כי אף אחד
    // מהם עדיין לא נוצר), ואז *שניהם* ייצרו לעצמם session נפרד - ולעולם לא
    // ייפגשו. הבחירה כאן היא "הראשון שנמצא בסריקה" - לא תור FIFO אמיתי לפי
    // כמה זמן מישהו/י ממתין/ה; מספיק טוב לשלב הזה.
    // matchmakingSessionIds.contains(entry.getKey()) - בלי זה, isWaitingForOpponent()
    // לבד לא מבחין בין "ממתין/ה כי לחצתי Play" לבין "ממתין/ה כי פתחתי חדר
    // פרטי ומחכה שחברה ספציפית תעשה Join" - שני המצבים נראים זהים מבחינת
    // GameSession עצמו (רק צד אחד מחובר). ה-Set מבטיח ש-Play יתאים רק
    // ל-session שגם הוא נוצר במקור דרך Play.
    private String resolveMatchmakingGameId(String searcherUsername) {
        Integer searcherElo = eloFor(searcherUsername).orElse(null);
        synchronized (sessionAllocationLock) {
            for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
                GameSession candidate = entry.getValue();
                if (!matchmakingSessionIds.contains(entry.getKey()) || !candidate.isWaitingForOpponent()) {
                    continue;
                }
                Integer candidateElo = eloFor(candidate.waitingPlayerUsername().orElse(null)).orElse(null);
                if (isCompatibleElo(searcherElo, candidateElo)) {
                    matchmakingDeadlines.remove(entry.getKey()); // מצא/ה match - בטל את ה-timeout
                    return entry.getKey();
                }
            }
            String newGameId = "match-" + UUID.randomUUID();
            matchmakingSessionIds.add(newGameId);
            matchmakingDeadlines.put(newGameId, System.currentTimeMillis() + MATCHMAKING_TIMEOUT_MILLIS);
            sessions.computeIfAbsent(newGameId, id -> new GameSession(accountRepository));
            return newGameId;
        }
    }

    // ה-ELO הנוכחי של username נתון, או Optional.empty() אם אין username
    // (התחברות אנונימית) או שהחשבון לא נמצא - שני המצבים מטופלים באותה
    // צורה ע"י isCompatibleElo (fallback סובלני, ר' שם).
    private Optional<Integer> eloFor(String username) {
        return username == null ? Optional.empty() : accountRepository.currentElo(username);
    }

    // true אם ההפרש בין שני ה-ELO-ים הוא ≤100 (הדרישה המדויקת - "±100") -
    // *או* אם אין מספיק מידע לשפוט בכלל (מישהו/י מהשניים לא מחובר/ת עם
    // login, או שה-ELO שלו/ה לא נמצא). ה-fallback הזה נבחר בכוונה: בלי
    // login (למשל חיבור אנונימי לבדיקות) matchmaking היה נתקע לגמרי בלי
    // דרך להתאים, וזה גרוע יותר מהתאמה בלי בדיקת ELO.
    private boolean isCompatibleElo(Integer searcherElo, Integer candidateElo) {
        if (searcherElo == null || candidateElo == null) {
            return true;
        }
        return Math.abs(searcherElo - candidateElo) <= ELO_MATCH_RANGE;
    }

    // נקראת מ-tickAllSessions על כל session שיש לו דדליין רשום ב-
    // matchmakingDeadlines: אם כבר לא ממתין (מישהו/י הצטרף/ה, או שהמשחק
    // "נגמר" איכשהו) - רק מנקה את הרישום, בלי לשלוח כלום. אם עדיין ממתין/ה
    // וגם עבר הדדליין - שולחת MATCHMAKING_TIMEOUT ישירות לחיבור היחיד
    // שכבר שם (ר' session.connections() - יש בדיוק חיבור אחד במצב הזה),
    // ומנקה את הרישום כדי לא לשלוח שוב בכל טיק עוקב.
    private void checkMatchmakingTimeout(String gameId, GameSession session) {
        Long deadline = matchmakingDeadlines.get(gameId);
        if (deadline == null) {
            return;
        }
        if (!session.isWaitingForOpponent()) {
            matchmakingDeadlines.remove(gameId);
            return;
        }
        if (System.currentTimeMillis() < deadline) {
            return;
        }
        matchmakingDeadlines.remove(gameId);
        MatchmakingTimeoutMessage timeoutMessage = new MatchmakingTimeoutMessage(
                "Could not find a match with a compatible ELO within 1 minute.");
        session.connections().keySet().forEach(conn -> conn.send(gson.toJson(timeoutMessage)));
        fileLogger.log("Matchmaking timed out: gameId=" + gameId);
    }

    // מייצרת gameId חדש וייחודי בעזרת RoomIdGenerator (קוד קצר, קריא -
    // בניגוד ל-UUID של matchmaking, כי הקוד הזה כן צריך להיכתב/להיאמר
    // בין אנשים בפועל - ר' תיעוד RoomIdGenerator), ופותחת עבורו session
    // חדש. synchronized(sessionAllocationLock): בלי זה, שני לחיצות Create
    // כמעט-בו-זמניות (תיאורטי, נדיר מאוד עם 6 תווים אקראיים) עלולות תיאורטית
    // "לנחש" את אותו קוד ולדרוס אחת את השנייה בין הבדיקה ליצירה בפועל.
    private String createNewRoomGameId() {
        synchronized (sessionAllocationLock) {
            String newGameId;
            do {
                newGameId = RoomIdGenerator.generate();
            } while (sessions.containsKey(newGameId));
            sessions.computeIfAbsent(newGameId, id -> new GameSession(accountRepository));
            return newGameId;
        }
    }

    // ניתוק: מעביר ל-GameSession.handleDisconnect (במקום removeConnection הישנה)
    // כדי ש-WHITE/BLACK באמצע משחק פעיל יקבלו "חלון חסד" לחיבור מחדש
    // (שלב 5, auto-resign) במקום להפסיד/להיתקע מיד - ר' תיעוד handleDisconnect.
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String gameId = gameIdByConnection.remove(conn);
        fileLogger.log("Connection closed: gameId=" + gameId + ", code=" + code + ", reason=" + reason);
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
        fileLogger.log("ERROR: " + ex.getMessage());
    }

    // רץ אך ורק על thread הטיק: מקדם את כל המשחקים הפעילים לפי הזמן שעבר,
    // בודק אם משחקי matchmaking שממתינים עברו את דקת ה-timeout (ר'
    // checkMatchmakingTimeout - דילוג מיידי ל-sessions בלי דדליין רשום,
    // ר' matchmakingDeadlines.get שם), ואז משדר לכל אחד את מצבו.
    private void tickAllSessions() {
        long now = System.nanoTime();
        long elapsedMillis = (now - lastTickNanos) / 1_000_000;
        lastTickNanos = now;

        for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
            GameSession session = entry.getValue();
            session.tick(elapsedMillis);
            if (!matchmakingDeadlines.isEmpty()) {
                checkMatchmakingTimeout(entry.getKey(), session);
            }
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
