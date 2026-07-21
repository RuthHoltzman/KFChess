package kfchess.net;

import kfchess.bus.EventBus;
import kfchess.engine.GameEngine;
import kfchess.engine.NetworkActions;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.io.BoardParser;
import kfchess.model.Board;
import kfchess.model.Game;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
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

    private final Board board;
    private final GameEngine engine;
    private final NetworkActions networkActions;
    private final Map<WebSocket, ClientRole> connections = new ConcurrentHashMap<>();
    private final Queue<PendingCommand> pendingCommands = new ConcurrentLinkedQueue<>();

    // בנאי: בונה לוח פתיחה סטנדרטי + מנוע משחק טרי, בדיוק כמו GameWindowMain.GameSession המקומי.
    public GameSession() {
        this.board = new BoardParser(new Scanner(STARTING_BOARD_TEXT)).readBoard();
        Game game = new Game(board);
        this.engine = new GameEngine(game, new RuleEngine(), new RaelTime(), new EventBus());
        this.networkActions = new NetworkActions(engine);
    }

    // הראשון שמתחבר מקבל WHITE, השני BLACK, כל השאר SPECTATOR; synchronized כדי שלא ייכנסו שני "ראשונים" בו-זמנית.
    public synchronized ClientRole assignRole(WebSocket connection) {
        boolean whiteTaken = connections.containsValue(ClientRole.WHITE);
        boolean blackTaken = connections.containsValue(ClientRole.BLACK);
        ClientRole role = !whiteTaken ? ClientRole.WHITE : !blackTaken ? ClientRole.BLACK : ClientRole.SPECTATOR;
        connections.put(connection, role);
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
        ClientRole role = connections.get(pending.connection());
        if (role == null || !pending.command().isValid()) {
            return;
        }
        role.toPieceColor().ifPresent(color -> {
            Position target = new Position(pending.command().row(), pending.command().col());
            switch (pending.command().type()) {
                case CLICK -> networkActions.handleClick(color, target);
                case JUMP -> networkActions.handleJump(color, target);
            }
        });
    }

    // עותק הגנתי של כל החיבורים הפעילים - נחוץ ל-GameServer כדי לדעת למי לשדר snapshot.
    public Map<WebSocket, ClientRole> connections() {
        return Map.copyOf(connections);
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

        return new SnapshotMessage(scan.pieces(), selected.orElse(null), legalMoves,
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
