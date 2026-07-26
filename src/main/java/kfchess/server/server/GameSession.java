package kfchess.server.server;

import kfchess.account.AccountRepository;
import kfchess.account.EloCalculator;
import kfchess.bus.EventBus;
import kfchess.bus.GameLifecycleEvent;
import kfchess.engine.GameEngine;
import kfchess.engine.NetworkActions;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.io.BoardParser;
import kfchess.model.Board;
import kfchess.model.Game;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.server.ClientCommand;
import kfchess.server.ClientCommandType;
import kfchess.server.ClientRole;
import kfchess.server.JumpDto;
import kfchess.server.PieceDto;
import kfchess.server.SnapshotMessage;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * משחק בודד על השרת: עוטף Board+Game+GameEngine+NetworkActions משלו,
 * ואת רשימת חיבורי ה-WebSocket שמשתתפים בו (לבן/שחור/צופים).
 * <p>
 * כל שינוי במצב המשחק (tick) חייב לקרות מ-thread אחד בלבד - זה מה
 * שהופך את השרת ל"חד-תהליכי" מבחינת לוגיקת המשחק (ר' PROGRESS.md).
 * לכן פקודות שמגיעות מ-threads הרשת של Java-WebSocket (onMessage) לא
 * מבצעות שום דבר בעצמן - הן רק נכנסות לתור (pendingCommands), וה-tick
 * הוא זה שמרוקן ומבצע אותן, בדיוק לפי הדפוס המקובל לשרתי משחק
 * בזמן-אמת (queue של פקודות נכנסות, מעובד פעם אחת בכל טיק).
 */
public class GameSession {

    private static final String STARTING_BOARD_TEXT = """
            Board:
            bR bN bB bQ bK bB bN bR
            bP bP bP bP bP bP bP bP
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            wP wP wP wP wP wP wP wP
            wR wN wB wQ wK wB wN wR
            """;

    /** פקודה שהתקבלה מחיבור נתון וממתינה לעיבוד ב-tick הבא. */
    private record PendingCommand(WebSocket connection, ClientCommand command) {
    }

    /** חיבור + התפקיד שקיבל + ה-username שהזדהה איתו (null אם התחבר בלי login - ר' UsernameResolver). */
    private record ConnectedPlayer(ClientRole role, String username) {
    }

    /**
     * "חלון חסד" פתוח על תפקיד WHITE/BLACK אחרי שהחיבור החי שלו נסגר
     * באמצע משחק פעיל (ר' processDisconnections) - שומר את ה-username
     * כדי לזהות reconnect (ר' tryReconnect) ואת הרגע שבו פג הזמן, לפי
     * engine.now() (שעון המשחק, לא שעון-קיר) - בדיוק כמו כל שאר לוגיקת
     * הזמן במשחק (ר' PieceTimers/CaptureEffectTracker), כדי שהכול יתקדם
     * מ-thread הטיק היחיד ובלי תלות בזמן-קיר אמיתי (גם נוח יותר לבדיקה:
     * GameSessionTest יכול "לקפוץ" 20 שניות קדימה ב-tick() אחד, בלי sleep אמיתי).
     */
    private record PendingDisconnect(String username, long deadlineMillis) {
    }

    // 20 שניות - כמה זמן שמור לצד שהתנתק לחזור (אותו username בדיוק)
    // לפני שהיריב מוכרז כמנצח אוטומטית. אושר עם רות.
    private static final long DISCONNECT_GRACE_MILLIS = 20_000;

    // לא-final מרגע שנוסף RESTART: resetGame() בונה לוח/מנוע חדשים לגמרי
    // לתוך אותם שדות - ר' resetGame() למטה. עד אז (Part A/B) היו final,
    // כי המשחק תמיד היה נבנה פעם אחת ולא מתאפס.
    private Board board;
    private GameEngine engine;
    private NetworkActions networkActions;
    private final EventBus bus = new EventBus();
    private final AccountRepository accountRepository;
    private final Map<WebSocket, ConnectedPlayer> connections = new ConcurrentHashMap<>();
    // WHITE/BLACK בלבד יכולים להופיע כאן (ר' handleDisconnect - צופה
    // מוסר מיד, בלי חלון חסד). EnumMap כי המפתח סגור לשני ערכים בלבד,
    // אותו עיקרון כמו restartVotes למטה.
    private final Map<ClientRole, PendingDisconnect> pendingDisconnects = new EnumMap<>(ClientRole.class);
    // חיבורים שנסגרו ועדיין לא עובדו - בדיוק כמו pendingCommands: handleDisconnect
    // (thread הרשת, ר' GameServer.onClose) רק מתייקת לכאן, ו-tick() (thread
    // הטיק היחיד) הוא זה שבאמת קורא ל-engine.isGameOver()/engine.now() ומחליט
    // מה לעשות (ר' processDisconnections) - אסור לגעת ב-engine משום thread אחר.
    private final Queue<WebSocket> pendingDisconnections = new ConcurrentLinkedQueue<>();
    private final Queue<PendingCommand> pendingCommands = new ConcurrentLinkedQueue<>();
    // מי (WHITE/BLACK) כבר ביקש/ה RESTART מאז שהמשחק הנוכחי נגמר - נמחק
    // (clear()) בכל resetGame(). Set ולא boolean יחיד לכל צד, כי צריך
    // לזהות "שני הצדדים ביקשו" (לא רק "מישהו ביקש") - ר' applyCommand.
    private final Set<ClientRole> restartVotes = EnumSet.noneOf(ClientRole.class);

    // איזה טקסט-לוח resetGame() בונה ממנו - STARTING_BOARD_TEXT בכל
    // הבנאים הציבוריים (המשחק האמיתי); בנאי הבדיקה למטה יכול להחליף
    // את זה בלוח קטן/מותאם, כדי ש-GameSessionTest יוכל להגיע ל"המשחק
    // נגמר" במהלך אחד בודד במקום לשחק משחק שלם על 32 כלים.
    private final String boardText;

    // בנאי ישן, בלי עדכון ELO בכלל (accountRepository=null) - נשאר כדי
    // ש-GameSessionTest הקיים ימשיך לעבוד בלי שינוי; משחק בלי repository
    // עדיין תקין לגמרי, רק מדלג על עדכון ELO בסוף (ר' onGameLifecycleEvent).
    public GameSession() {
        this(null);
    }

    // בנאי: ה-bus נוצר *פעם אחת* לכל חיי ה-GameSession (לא בכל resetGame) -
    // כדי שההרשמה ל-GameLifecycleEvent(ENDED) (לעדכון ELO) תישאר תקפה גם
    // אחרי restart, בלי צורך להירשם מחדש בכל פעם. resetGame() עצמה בונה
    // רק את הלוח/מנוע/networkActions מחדש, על אותו bus.
    public GameSession(AccountRepository accountRepository) {
        this(STARTING_BOARD_TEXT, accountRepository);
    }

    // לבדיקות בעיקר (public כי GameSessionTest חי בחבילה texttests, לא
    // kfchess.net.server - package-private לא היה נגיש משם): מזריקה
    // טקסט-לוח מותאם אישית במקום STARTING_BOARD_TEXT - אותו עיקרון בדיוק
    // כמו הזרקת RuleEngine/RaelTime/EventBus/AccountRepository דרך הבנאי,
    // רק שגם "מה הלוח" הופך לניתן-להזרקה. כך GameSessionTest יכול לבנות
    // לוח שבו לכידת מלך היא מהלך אחד בודד, ולבדוק בפועל את applyRestartVote
    // (שדורש שהמשחק *כבר* נגמר), בלי לשחק משחק אמיתי על 32 כלים.
    public GameSession(String boardText, AccountRepository accountRepository) {
        this.boardText = boardText;
        this.accountRepository = accountRepository;
        bus.subscribe(GameLifecycleEvent.class, this::onGameLifecycleEvent);
        resetGame();
    }

    // בונה לוח פתיחה (STARTING_BOARD_TEXT בייצור, או הלוח שהוזרק לבדיקה)
    // + מנוע משחק טרי - נקראת מהבנאי (משחק ראשון) וגם מ-applyCommand
    // כששני הצדדים ביקשו RESTART (משחק הבא, על אותו boardText בדיוק).
    // לא מאפסת connections/accountRepository/bus בכלל - אלה שייכים
    // ל"מושב" (session) עצמו, לא למשחק הבודד שרץ בתוכו.
    private void resetGame() {
        this.board = new BoardParser(new Scanner(boardText)).readBoard();
        Game game = new Game(board);
        this.engine = new GameEngine(game, new RuleEngine(), new RaelTime(), bus);
        this.networkActions = new NetworkActions(engine);
        restartVotes.clear();
    }

    // בנאי ישן בלי username - נשאר כדי ש-GameSessionTest הקיים ימשיך לעבוד;
    // מתאים גם לחיבורים בלי login בכלל (בדיקת פרוטוקול גולמי).
    public synchronized ClientRole assignRole(WebSocket connection) {
        return assignRole(connection, null);
    }

    // הראשון שמתחבר מקבל WHITE, השני BLACK, כל השאר SPECTATOR; synchronized כדי שלא ייכנסו שני "ראשונים" בו-זמנית.
    // username נשמר יחד עם התפקיד כדי ש-onGameLifecycleEvent ידע בסוף המשחק למי לעדכן ELO.
    // קודם בודקים reconnect (ר' tryReconnect) - חיבור חדש עם username שתואם
    // בדיוק למי שנמצא כרגע ב"חלון חסד" (ר' handleDisconnect/pendingDisconnects)
    // מקבל בחזרה את אותו תפקיד בדיוק, במקום להיחשב כחיבור "רגיל".
    public synchronized ClientRole assignRole(WebSocket connection, String username) {
        Optional<ClientRole> reconnected = tryReconnect(connection, username);
        if (reconnected.isPresent()) {
            return reconnected.get();
        }
        boolean whiteTaken = isRoleOccupied(ClientRole.WHITE);
        boolean blackTaken = isRoleOccupied(ClientRole.BLACK);
        ClientRole role = !whiteTaken ? ClientRole.WHITE : !blackTaken ? ClientRole.BLACK : ClientRole.SPECTATOR;
        connections.put(connection, new ConnectedPlayer(role, username));
        return role;
    }

    // "תפוס" = יש עליו חיבור חי, *או* הוא שמור לצד שהתנתק ועדיין בתוך
    // חלון החסד - כדי שאף אחד אחר לא "יגנוב" את הצבע שהתפנה תוך כדי
    // שהשחקן המקורי עדיין עשוי לחזור.
    private boolean isRoleOccupied(ClientRole role) {
        return connections.values().stream().anyMatch(p -> p.role() == role) || pendingDisconnects.containsKey(role);
    }

    // אם יש חלון-חסד פתוח (על WHITE או BLACK) עם בדיוק אותו username -
    // זה חיבור-מחדש: מבטלים את חלון החסד ומחזירים לחיבור החדש את אותו
    // תפקיד. username=null (לא מזוהה בכלל) אף פעם לא "מזהה" reconnect -
    // אחרת כל חיבור אנונימי היה תופס בטעות מקום ששמור למישהו מזוהה.
    private Optional<ClientRole> tryReconnect(WebSocket connection, String username) {
        if (username == null) {
            return Optional.empty();
        }
        for (Map.Entry<ClientRole, PendingDisconnect> entry : pendingDisconnects.entrySet()) {
            if (username.equals(entry.getValue().username())) {
                ClientRole role = entry.getKey();
                pendingDisconnects.remove(role);
                connections.put(connection, new ConnectedPlayer(role, username));
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    // מסיר חיבור שהתנתק - לא משפיע על צבעים תפוסים אחרים (אין "פינוי מקום" ליריב שכבר מחובר).
    // ציבורית ונשארת כמו שהיא (בלי לוגיקת חלון-חסד בכלל) כי GameSessionTest
    // הקיים קורא לה ישירות; handleDisconnect למטה היא הכניסה האמיתית
    // מ-GameServer.onClose, ומוסיפה מעליה את לוגיקת חלון-החסד.
    public void removeConnection(WebSocket connection) {
        connections.remove(connection);
    }

    // נקראת מ-thread הרשת (GameServer.onClose). לא נוגעת ב-connections/engine
    // בעצמה בכלל - רק מתייקת את החיבור, בדיוק כמו ש-enqueueCommand מתייקת
    // פקודות (ר' תיעוד pendingDisconnections למעלה) - העיבוד האמיתי קורה
    // ב-processDisconnections, מ-thread הטיק בלבד.
    public void handleDisconnect(WebSocket connection) {
        pendingDisconnections.add(connection);
    }

    // נקרא מ-thread הרשת (onMessage): רק מכניס לתור, לא נוגע ב-engine בכלל.
    public void enqueueCommand(WebSocket connection, ClientCommand command) {
        pendingCommands.add(new PendingCommand(connection, command));
    }

    // נקרא רק מה-thread היחיד של לולאת הטיק: קודם מעבד ניתוקים שהצטברו
    // (processDisconnections - חייב לקרות לפני applyCommand, כדי שפקודה
    // שהגיעה מחיבור שכבר נסגר לא "תעבור" רק כי עדיין לא ניקינו אותו),
    // אז את כל הפקודות הרגילות, מקדם את שעון המשחק, ולבסוף בודק אם
    // חלון-חסד כלשהו פג (resolveExpiredDisconnects) - הכול על אותו
    // thread יחיד בדיוק, בלי Timer/thread נפרד.
    // synchronized (חדש, שלב 5): pendingDisconnects (EnumMap, לא thread-safe
    // כמו connections/pendingCommands) נקרא/נכתב גם כאן (מ-thread הטיק)
    // וגם מ-assignRole (מ-thread הרשת, ר' tryReconnect/isRoleOccupied) -
    // שני המתודות חייבות לחלוק את אותו מנעול (this) כדי שלא יתנגשו.
    public synchronized void tick(long elapsedMillis) {
        processDisconnections();
        PendingCommand pending;
        while ((pending = pendingCommands.poll()) != null) {
            applyCommand(pending);
        }
        engine.handleWait(elapsedMillis);
        resolveExpiredDisconnects();
    }

    // מעבד את כל הניתוקים שהצטברו מאז הטיק הקודם (ר' handleDisconnect) -
    // כאן, ורק כאן, מותר לגעת ב-connections/engine בעקבות ניתוק. לכל
    // חיבור: אם היה צופה, או שהמשחק כבר נגמר - מוסר בלבד (removeConnection,
    // כמו התנהגות ה-onClose המקורית). אחרת (WHITE/BLACK באמצע משחק פעיל) -
    // מוסיר את החיבור המת אבל *שומר* את התפקיד+username ב-pendingDisconnects
    // עם דדליין DISCONNECT_GRACE_MILLIS קדימה על שעון המשחק (engine.now()) -
    // resolveExpiredDisconnects למטה בודק את זה בכל טיק.
    private void processDisconnections() {
        WebSocket connection;
        while ((connection = pendingDisconnections.poll()) != null) {
            ConnectedPlayer player = connections.get(connection);
            removeConnection(connection);
            if (player != null && player.role() != ClientRole.SPECTATOR && !engine.isGameOver()) {
                pendingDisconnects.put(player.role(),
                        new PendingDisconnect(player.username(), engine.now() + DISCONNECT_GRACE_MILLIS));
            }
        }
    }

    // אם WHITE/BLACK כלשהו נמצא מעבר לדדליין שלו ב-pendingDisconnects -
    // ההפסד קורה עכשיו: הצבע השני (opposite) מוכרז כמנצח דרך
    // engine.forceGameOver - אותה נקודה משותפת שגם לכידת מלך משתמשת בה
    // (ר' GameEngine.forceGameOver), כדי שעדכון ה-ELO הקיים (מאזין ל-
    // GameLifecycleEvent(ENDED), ר' onGameLifecycleEvent) יקרה בלי לכתוב
    // אותו שוב. usernameFor(loserRole) בתוך onGameLifecycleEvent חייב
    // עדיין למצוא את ה-username דרך pendingDisconnects (ר' usernameFor
    // למטה) - זו הסיבה ש-clear() קורה *אחרי* forceGameOver ולא לפניו.
    // בודקת engine.isGameOver() קודם כדי לא "לדרוס" סיום משחק שכבר קרה
    // (למשל לכידת מלך רגילה) באותו טיק.
    private void resolveExpiredDisconnects() {
        if (engine.isGameOver() || pendingDisconnects.isEmpty()) {
            return;
        }
        long now = engine.now();
        for (Map.Entry<ClientRole, PendingDisconnect> entry : pendingDisconnects.entrySet()) {
            if (now >= entry.getValue().deadlineMillis()) {
                PieceColor winner = entry.getKey().toPieceColor().orElseThrow().opposite();
                engine.forceGameOver(winner);
                pendingDisconnects.clear();
                return;
            }
        }
    }

    // מנתב פקודה בודדת לפי הצבע ששויך לחיבור ששלח אותה; מתעלם משולח לא-מזוהה או צופה.
    // בקשת רות: כל עוד ממתינים ליריב (isWaitingForOpponent) - צד יחיד
    // שכבר מחובר לא יכול "להתחיל לשחק" (CLICK/JUMP מתעלמים בשקט, בלי
    // הודעת שגיאה ללקוח - פשוט לא קורה כלום). נבדק *אחרי* RESTART בכוונה:
    // RESTART ממילא רלוונטי רק כש-engine.isGameOver() (ר' applyRestartVote),
    // ואם המשחק נגמר isWaitingForOpponent() תמיד false בכל מקרה - אין כאן
    // התנגשות אמיתית, רק סדר בדיקות הגיוני (קודם המקרה המיוחד, RESTART).
    // isWaitingForOpponent() היא synchronized(this) - קריאה מכאן בטוחה
    // כי applyCommand תמיד רץ מתוך tick(), שכבר מחזיק את אותו מנעול
    // (נעילה חוזרת/reentrant, לא deadlock).
    private void applyCommand(PendingCommand pending) {
        ConnectedPlayer player = connections.get(pending.connection());
        if (player == null || !pending.command().isValid()) {
            return;
        }
        if (pending.command().type() == ClientCommandType.RESTART) {
            applyRestartVote(player.role());
            return;
        }
        if (isWaitingForOpponent()) {
            return;
        }
        player.role().toPieceColor().ifPresent(color -> {
            Position target = new Position(pending.command().row(), pending.command().col());
            switch (pending.command().type()) {
                case CLICK -> networkActions.handleClick(color, target);
                case JUMP -> networkActions.handleJump(color, target);
            }
        });
    }

    // מטפל בבקשת RESTART מצד אחד: מתעלם אם המשחק עדיין לא נגמר (אחרת
    // אפשר "לברוח" מהפסד ע"י איפוס הלוח), ומתעלם מצופה (אין לו/ה בכלל
    // צד לייצג). כשגם WHITE וגם BLACK ביקשו - מאפס בפועל (ר' resetGame).
    private void applyRestartVote(ClientRole role) {
        if (!engine.isGameOver() || role == ClientRole.SPECTATOR) {
            return;
        }
        restartVotes.add(role);
        if (restartVotes.contains(ClientRole.WHITE) && restartVotes.contains(ClientRole.BLACK)) {
            resetGame();
        }
    }

    // "ממתין/ה ליריב" - נחוץ ל-matchmaking (שלב 5 חלק 2, כפתור "Skip" ב-
    // HomeScreenMain/GameServer.resolveMatchmakingGameId): true רק אם יש
    // *בדיוק* צד אחד (WHITE או BLACK) מחובר בפועל, והצד השני **לא** מחובר
    // *וגם* לא שמור לו חלון-חסד (ר' pendingDisconnects, שלב 5 חלק 1) - כדי
    // שמי שממתין/ה לחיבור-מחדש של היריב המקורי (אחרי ניתוק) לא "תיחטף"
    // בטעות ע"י שחקן/ית אקראי/ת מה-matchmaking. synchronized כי היא נקראת
    // מ-thread הרשת (בזמן שסורקים sessions קיימים) וקוראת מ-pendingDisconnects,
    // בדיוק כמו tick()/snapshotFor - אותו מנעול.
    public synchronized boolean isWaitingForOpponent() {
        if (engine.isGameOver()) {
            return false;
        }
        boolean whiteConnected = connections.values().stream().anyMatch(p -> p.role() == ClientRole.WHITE);
        boolean blackConnected = connections.values().stream().anyMatch(p -> p.role() == ClientRole.BLACK);
        boolean whiteOpen = !whiteConnected && !pendingDisconnects.containsKey(ClientRole.WHITE);
        boolean blackOpen = !blackConnected && !pendingDisconnects.containsKey(ClientRole.BLACK);
        return (whiteConnected && blackOpen) || (blackConnected && whiteOpen);
    }

    // ה-username של הצד היחיד שכבר מחובר, כש-isWaitingForOpponent()=true
    // (אחרת Optional.empty()) - נחוץ ל-GameServer.resolveMatchmakingGameId
    // כדי לבדוק התאמת ELO מול מי שמחפש/ת משחק חדש/ה (תיקון "Play" לפי
    // המפרט המדויק - "ELO in range of ±100"). Optional.empty() גם אם
    // מי שממתין/ה בכלל לא התחבר/ה עם login (username=null) - GameServer
    // מטפל בזה כ"אין מספיק מידע, לא חוסמים את ההתאמה" (fallback סובלני),
    // לא כאן. synchronized: קוראת ל-isWaitingForOpponent() (נעילה חוזרת,
    // לא deadlock) וגם ל-connections - אותו מנעול כמו כל שאר המתודות כאן.
    public synchronized Optional<String> waitingPlayerUsername() {
        if (!isWaitingForOpponent()) {
            return Optional.empty();
        }
        return connections.values().stream().findFirst().map(ConnectedPlayer::username);
    }

    // עותק הגנתי של כל החיבורים הפעילים - נחוץ ל-GameServer כדי לדעת למי לשדר snapshot.
    // מחזיר Map<WebSocket, ClientRole> (לא ConnectedPlayer) בכוונה - זה כל
    // מה ש-GameServer.broadcast/הטסטים הקיימים צריכים לדעת, ה-username הוא
    // פרט פנימי של GameSession בלבד (ר' onGameLifecycleEvent).
    public Map<WebSocket, ClientRole> connections() {
        Map<WebSocket, ClientRole> roles = new HashMap<>();
        connections.forEach((connection, player) -> roles.put(connection, player.role()));
        return roles;
    }

    // נקרא (מ-thread הטיק, סינכרונית מתוך bus.publish בתוך GameEngine) בדיוק
    // פעם אחת ברגע שהמשחק נגמר (לכידת מלך) - לא בכל tick. מזהה מי היה
    // WHITE/BLACK *באותו רגע* (ר' תיעוד ConnectedPlayer) ומעדכן ELO לשניהם
    // דרך AccountRepository. מדלג בשקט (בלי שגיאה) בכל מקרה שבו אי אפשר
    // לחשב דירוג משמעותי: אין repository בכלל (בנאי הישן, טסטים), אחד
    // הצדדים לא היה מזוהה (login), או ששני הצדדים אותו username (בדיקה
    // עצמית - אי אפשר לדרג נגד עצמך).
    private void onGameLifecycleEvent(GameLifecycleEvent event) {
        if (accountRepository == null || event.phase() != GameLifecycleEvent.Phase.ENDED) {
            return;
        }
        ClientRole winnerRole = event.winner() == PieceColor.WHITE ? ClientRole.WHITE : ClientRole.BLACK;
        ClientRole loserRole = winnerRole == ClientRole.WHITE ? ClientRole.BLACK : ClientRole.WHITE;
        String winnerUsername = usernameFor(winnerRole);
        String loserUsername = usernameFor(loserRole);
        if (winnerUsername == null || loserUsername == null || winnerUsername.equals(loserUsername)) {
            return;
        }

        accountRepository.currentElo(winnerUsername).ifPresent(winnerElo ->
                accountRepository.currentElo(loserUsername).ifPresent(loserElo -> {
                    int[] updated = EloCalculator.applyResult(winnerElo, loserElo);
                    accountRepository.updateElo(winnerUsername, updated[0]);
                    accountRepository.updateElo(loserUsername, updated[1]);
                }));
    }

    // ה-username של מי שמחזיק כרגע בתפקיד הנתון, או null אם אין כזה (לא
    // מחובר בכלל, או התחבר בלי username - ר' UsernameResolver). כשהתפקיד
    // הוא של צד שהתנתק ממש עכשיו (ר' resolveExpiredDisconnects שקורא
    // לכאן דרך GameLifecycleEvent, לפני שה-pending נמחק) - אין לו יותר
    // חיבור חי ב-connections בכלל, אז נופלים חזרה ל-pendingDisconnects
    // כדי שעדכון ה-ELO עדיין ידע למי להוריד דירוג.
    private String usernameFor(ClientRole role) {
        return connections.values().stream()
                .filter(player -> player.role() == role)
                .map(ConnectedPlayer::username)
                .findFirst()
                .or(() -> Optional.ofNullable(pendingDisconnects.get(role)).map(PendingDisconnect::username))
                .orElse(null);
    }

    // שניות שנותרו עד שהצד שהתנתק (אם יש כזה) יפסיד אוטומטית - null אם
    // אף אחד לא נמצא כרגע ב"חלון חסד". בכוונה לא תלוי ב-viewerRole (בניגוד
    // ל-restartRequestedByViewer) - זו הודעה זהה לכולם (כולל צופים), לא הצבעה אישית.
    private Integer disconnectSecondsRemaining() {
        if (pendingDisconnects.isEmpty()) {
            return null;
        }
        long now = engine.now();
        long soonestDeadline = pendingDisconnects.values().stream()
                .mapToLong(PendingDisconnect::deadlineMillis)
                .min()
                .orElseThrow();
        long remainingMillis = Math.max(0, soonestDeadline - now);
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    /** תוצאת סריקת הלוח: הכלים לשידור + מיקום כל כלי (זהות, לא ערך) - דרוש כדי לאתר קפיצות (ר' collectJumps). */
    private record BoardScan(List<PieceDto> pieces, Map<Piece, Position> positionByPiece) {
    }

    // בונה את הודעת המצב (snapshot) עבור צופה ספציפי - selected/legalMoves הם רק ביחס לצבע שלו.
    // motions/jumps/captureEffects זהים לכל הצופים - זה בדיוק מה שה-UI המקומי מצייר כאנימציה.
    // synchronized (חדש, שלב 5): disconnectSecondsRemaining() קורא מ-pendingDisconnects,
    // שגם assignRole (thread הרשת) יכול לגעת בו בו-זמנית - אותו מנעול כמו tick()/assignRole.
    public synchronized SnapshotMessage snapshotFor(ClientRole viewerRole) {
        BoardScan scan = scanBoard();
        Optional<Position> selected = viewerRole.toPieceColor()
                .flatMap(networkActions::selectedPositionFor);
        List<Position> legalMoves = selected.map(engine::legalMovesFrom).orElse(List.of());
        String winner = engine.winner().map(PieceColor::name).orElse(null);
        // רלוונטי רק כש-gameOver - "כבר ביקשתי RESTART, מחכה לצד השני" -
        // תמיד false לצופה (SPECTATOR), כי אין לו/ה בכלל הצבעה על restart.
        boolean restartRequestedByViewer = restartVotes.contains(viewerRole);

        return new SnapshotMessage(board.width(), board.height(), scan.pieces(), selected.orElse(null), legalMoves,
                scoresByName(), moveLogByName(), engine.isGameOver(), winner, engine.now(),
                engine.activeMotions(), collectJumps(scan.positionByPiece()), engine.recentCaptureEffects(),
                restartRequestedByViewer, disconnectSecondsRemaining(), isWaitingForOpponent());
    }

    // סורק את כל הלוח (row/col) פעם אחת - אוסף גם PieceDto לשידור וגם piece->position לצורך collectJumps.
    private BoardScan scanBoard() {
        List<PieceDto> pieces = new ArrayList<>();
        Map<Piece, Position> positionByPiece = new HashMap<>();
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Position position = new Position(row, col);
                board.pieceAt(position).ifPresent(piece -> {
                    pieces.add(PieceDto.from(piece, position));
                    positionByPiece.put(piece, position);
                });
            }
        }
        return new BoardScan(pieces, positionByPiece);
    }

    // ממיר את כל הקפיצות הפעילות ל-DTO; JumpVisual לא יודע את מיקומו בעצמו, לכן משתמשים במפה מ-scanBoard.
    // (motions/captureEffects לא צריכים המרה כזו - engine.activeMotions()/recentCaptureEffects() כבר משודרים ישירות.)
    private List<JumpDto> collectJumps(Map<Piece, Position> positionByPiece) {
        List<JumpDto> jumps = new ArrayList<>();
        for (JumpVisual jump : engine.activeJumps()) {
            Position position = positionByPiece.get(jump.piece());
            if (position != null) {
                jumps.add(JumpDto.from(jump, position));
            }
        }
        return jumps;
    }

    // ממיר Map<PieceColor,Integer> של הניקוד ל-Map<String,Integer> לפי שם הצבע, לשידור ב-JSON.
    private Map<String, Integer> scoresByName() {
        Map<String, Integer> byName = new HashMap<>();
        engine.scores().forEach((color, score) -> byName.put(color.name(), score));
        return byName;
    }

    // ממיר Map<PieceColor,List<String>> של יומן המהלכים ל-Map<String,List<String>>, לשידור ב-JSON.
    private Map<String, List<String>> moveLogByName() {
        Map<String, List<String>> byName = new HashMap<>();
        engine.moveLog().forEach((color, log) -> byName.put(color.name(), log));
        return byName;
    }
}
