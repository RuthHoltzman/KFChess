package kfchess.net.server;

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
import kfchess.net.ClientCommand;
import kfchess.net.ClientRole;
import kfchess.net.JumpDto;
import kfchess.net.PieceDto;
import kfchess.net.SnapshotMessage;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
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

    private final Board board;
    private final GameEngine engine;
    private final NetworkActions networkActions;
    private final AccountRepository accountRepository;
    private final Map<WebSocket, ConnectedPlayer> connections = new ConcurrentHashMap<>();
    private final Queue<PendingCommand> pendingCommands = new ConcurrentLinkedQueue<>();

    // בנאי ישן, בלי עדכון ELO בכלל (accountRepository=null) - נשאר כדי
    // ש-GameSessionTest הקיים ימשיך לעבוד בלי שינוי; משחק בלי repository
    // עדיין תקין לגמרי, רק מדלג על עדכון ELO בסוף (ר' onGameLifecycleEvent).
    public GameSession() {
        this(null);
    }

    // בנאי: בונה לוח פתיחה סטנדרטי + מנוע משחק טרי (אותה שיטת בנייה שהמשחק המקומי המקורי השתמש בה).
    // נרשם ל-GameLifecycleEvent(ENDED) של ה-bus שהוא עצמו יוצר ומעביר ל-GameEngine -
    // זה מה שמאפשר עדכון ELO בדיוק פעם אחת, ברגע שהמשחק נגמר בפועל (לא בכל tick).
    public GameSession(AccountRepository accountRepository) {
        this.board = new BoardParser(new Scanner(STARTING_BOARD_TEXT)).readBoard();
        Game game = new Game(board);
        EventBus bus = new EventBus();
        this.engine = new GameEngine(game, new RuleEngine(), new RaelTime(), bus);
        this.networkActions = new NetworkActions(engine);
        this.accountRepository = accountRepository;
        bus.subscribe(GameLifecycleEvent.class, this::onGameLifecycleEvent);
    }

    // בנאי ישן בלי username - נשאר כדי ש-GameSessionTest הקיים ימשיך לעבוד;
    // מתאים גם לחיבורים בלי login בכלל (בדיקת פרוטוקול גולמי).
    public synchronized ClientRole assignRole(WebSocket connection) {
        return assignRole(connection, null);
    }

    // הראשון שמתחבר מקבל WHITE, השני BLACK, כל השאר SPECTATOR; synchronized כדי שלא ייכנסו שני "ראשונים" בו-זמנית.
    // username נשמר יחד עם התפקיד כדי ש-onGameLifecycleEvent ידע בסוף המשחק למי לעדכן ELO.
    public synchronized ClientRole assignRole(WebSocket connection, String username) {
        boolean whiteTaken = connections.values().stream().anyMatch(p -> p.role() == ClientRole.WHITE);
        boolean blackTaken = connections.values().stream().anyMatch(p -> p.role() == ClientRole.BLACK);
        ClientRole role = !whiteTaken ? ClientRole.WHITE : !blackTaken ? ClientRole.BLACK : ClientRole.SPECTATOR;
        connections.put(connection, new ConnectedPlayer(role, username));
        return role;
    }

    // מסיר חיבור שהתנתק - לא משפיע על צבעים תפוסים אחרים (אין "פינוי מקום" ליריב שכבר מחובר).
    public void removeConnection(WebSocket connection) {
        connections.remove(connection);
    }

    // נקרא מ-thread הרשת (onMessage): רק מכניס לתור, לא נוגע ב-engine בכלל.
    public void enqueueCommand(WebSocket connection, ClientCommand command) {
        pendingCommands.add(new PendingCommand(connection, command));
    }

    // נקרא רק מה-thread היחיד של לולאת הטיק: מבצע קודם את כל הפקודות שהצטברו, ואז מקדם את שעון המשחק.
    public void tick(long elapsedMillis) {
        PendingCommand pending;
        while ((pending = pendingCommands.poll()) != null) {
            applyCommand(pending);
        }
        engine.handleWait(elapsedMillis);
    }

    // מנתב פקודה בודדת לפי הצבע ששויך לחיבור ששלח אותה; מתעלם משולח לא-מזוהה או צופה.
    private void applyCommand(PendingCommand pending) {
        ConnectedPlayer player = connections.get(pending.connection());
        if (player == null || !pending.command().isValid()) {
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
    // מחובר בכלל, או התחבר בלי username - ר' UsernameResolver).
    private String usernameFor(ClientRole role) {
        return connections.values().stream()
                .filter(player -> player.role() == role)
                .map(ConnectedPlayer::username)
                .findFirst()
                .orElse(null);
    }

    /** תוצאת סריקת הלוח: הכלים לשידור + מיקום כל כלי (זהות, לא ערך) - דרוש כדי לאתר קפיצות (ר' collectJumps). */
    private record BoardScan(List<PieceDto> pieces, Map<Piece, Position> positionByPiece) {
    }

    // בונה את הודעת המצב (snapshot) עבור צופה ספציפי - selected/legalMoves הם רק ביחס לצבע שלו.
    // motions/jumps/captureEffects זהים לכל הצופים - זה בדיוק מה שה-UI המקומי מצייר כאנימציה.
    public SnapshotMessage snapshotFor(ClientRole viewerRole) {
        BoardScan scan = scanBoard();
        Optional<Position> selected = viewerRole.toPieceColor()
                .flatMap(networkActions::selectedPositionFor);
        List<Position> legalMoves = selected.map(engine::legalMovesFrom).orElse(List.of());
        String winner = engine.winner().map(PieceColor::name).orElse(null);

        return new SnapshotMessage(board.width(), board.height(), scan.pieces(), selected.orElse(null), legalMoves,
                scoresByName(), moveLogByName(), engine.isGameOver(), winner, engine.now(),
                engine.activeMotions(), collectJumps(scan.positionByPiece()), engine.recentCaptureEffects());
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
