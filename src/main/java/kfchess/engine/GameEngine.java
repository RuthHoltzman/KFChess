package kfchess.engine;

import kfchess.model.Board;
import kfchess.model.Game;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import kfchess.realtime.Motion;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "המוח" של המשחק: מקבל אירועים ברמת התחום (קליק על תא, המתנה, קפיצה)
 * ומתרגם אותם לשינויים ב-Game/Board, תוך אכיפת חוקי התנועה דרך RuleEngine.
 * <p>
 * שימו לב: הכלי הנבחר (selection), רשימת המהלכים הפעילים (activeMotions)
 * ורשימת הקפיצות הפעילות (jumpEndTimes) הן state per-engine ולא סטטי
 * גלובלי - כל אלה מאפשרים בעתיד להריץ כמה משחקים/לוחות במקביל, וגם
 * מאפשרים לכמה כלים לזוז/לקפוץ בו-זמנית (בניגוד לקוד המקורי שתמך
 * בכלי אחד בתנועה ובקפיצה אחת בלבד באמצעות משתנים סטטיים).
 */
public class GameEngine {

    private static final long MILLISECONDS_PER_SQUARE = 1000;
    private static final long DEFAULT_JUMP_DURATION_MS = 1000;

    /**
     * כמה זמן (במילישניות) אפקט "לכידה" (ר' CaptureEffect) נשאר חי אחרי
     * שכלי הוסר מהלוח - חלון קצר שנועד רק לאפשר ל-UI לצייר אפקט חזותי
     * (למשל טבעת דוהה) במקום שבו הכלי נעלם, כדי שההיעלמות לא תיראה
     * כאילו "כלום לא קרה". ציבורי כדי ש-SnapshotFactory יוכל לחשב לפיו
     * את שבר ההתקדמות (progress) של כל אפקט, בלי לשכפל את המספר.
     */
    public static final long CAPTURE_EFFECT_DURATION_MS = 450;

    // משכי "מנוחה" (cooldown) אחרי הליכה/קפיצה - כמה זמן הכלי חסום מפעולה.
    // ציבוריים כי PieceVisualStateTracker משתמש באותם ערכים בדיוק כדי
    // שהאנימציה (שעון החול) תמיד תואמת בדיוק את משך הזמן שבו הכלי באמת
    // לא ניתן להזזה - אין שני מקורות אמת לאותו מספר.
    public static final long SHORT_REST_DURATION_MS = 500;  // אחרי הליכה
    public static final long LONG_REST_DURATION_MS = 1000;  // אחרי קפיצה

    private final Game game;
    private final RuleEngine ruleEngine;
    private final RaelTime clock;

    private final List<Motion> activeMotions = new ArrayList<>();
    private final Map<Piece, Long> jumpEndTimes = new HashMap<>();
    // זמני התחלה של קפיצות פעילות - נשמר במקביל ל-jumpEndTimes (ולא בתוכו),
    // כדי לא לגעת בלוגיקת הקפיצה הקיימת שכבר עובדת נכון; המפה הזו משרתת
    // אך ורק את שכבת התצוגה (חישוב קשת הגובה של הקפיצה ב-SnapshotFactory).
    private final Map<Piece, Long> jumpStartTimes = new HashMap<>();
    // כלים שהוסרו לאחרונה מהלוח (לכידה רגילה, או "התאדות" תוקף מול כלי
    // קופץ) - נשמר זמנית רק כדי שה-UI יוכל לצייר אפקט לכידה קצר; ר' תיעוד
    // מלא ב-CaptureEffect.
    private final List<CaptureEffect> recentCaptures = new ArrayList<>();
    // מתי המנוחה הנוכחית של כל כלי (אם יש) מסתיימת - כל עוד clock.now()
    // קטן מהערך הזה, הכלי לא זמין לבחירה/תנועה/קפיצה (ר' isAvailableToAct).
    // זה מה שהופך את "המנוחה" ממשהו ויזואלי-בלבד (שהיה קודם) לכלל משחק
    // אמיתי: קליק על כלי שנח לא בוחר אותו בכלל - בדיוק כמו כלי שנמצא
    // כרגע בתנועה (IN_TRANSIT) או בקפיצה (JUMPING).
    private final Map<Piece, Long> restEndTimes = new HashMap<>();
    private Position selectedPosition;

    // ניקוד ורשימת מהלכים לכל צבע - נאספים כאן (ולא ב-UI) כי הם חלק
    // מהתקדמות המשחק עצמה, וצריך שיהיו זמינים גם ל-ConsoleRunner/בדיקות,
    // לא רק לחלון ה-Swing.
    private final Map<PieceColor, Integer> scores = new EnumMap<>(PieceColor.class);
    private final Map<PieceColor, List<String>> moveLog = new EnumMap<>(PieceColor.class);

    public GameEngine(Game game, RuleEngine ruleEngine, RaelTime clock) {
        this.game = game;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
        for (PieceColor color : PieceColor.values()) {
            scores.put(color, 0);
            moveLog.put(color, new ArrayList<>());
        }
    }

    public boolean isGameOver() {
        return game.isGameOver();
    }

    /** הצבע שניצח, אם המשחק נגמר (ריק אם המשחק עדיין רץ). */
    public Optional<PieceColor> winner() {
        return game.winner();
    }

    public Board board() {
        return game.board();
    }

    public void handleClick(Position clicked) {
        advanceGameState();
        if (game.isGameOver() || !board().isWithinBounds(clicked)) {
            return;
        }
        if (selectedPosition == null) {
            trySelect(clicked);
        } else {
            tryActOnSelection(clicked);
        }
    }

    public void handleWait(long milliseconds) {
        if (game.isGameOver()) {
            return;
        }
        clock.advance(milliseconds);
        advanceGameState();
    }

    public void handleJump(Position target) {
        advanceGameState();
        if (game.isGameOver()) {
            return;
        }
        board().pieceAt(target).ifPresent(piece -> {
            if (isAvailableToAct(piece)) {
                piece.markJumping();
                long startTime = clock.now();
                jumpStartTimes.put(piece, startTime);
                jumpEndTimes.put(piece, startTime + DEFAULT_JUMP_DURATION_MS);
                // אם הכלי שקפץ היה הכלי הנבחר, יש לבטל את הבחירה: הוא כבר
                // לא IDLE, ולכן ממילא לא ניתן להזיז אותו - אבל בלי הביטול
                // הזה ה-UI היה ממשיך לצייר עליו מסגרת "נבחר" כאילו אפשר
                // עדיין לבחור לו יעד, מה שמטעה את השחקן.
                if (target.equals(selectedPosition)) {
                    selectedPosition = null;
                }
            }
        });
    }

    /**
     * האם כלי זמין כרגע לפעולה (בחירה/תנועה/קפיצה): לא רק "IDLE" ברמת
     * המודל, אלא גם לא נמצא כרגע ב"מנוחה" (cooldown) אחרי הליכה/קפיצה
     * קודמת. זה מה שהופך את שעון החול הצהוב מקישוט בלבד לכלל משחק אמיתי.
     */
    private boolean isAvailableToAct(Piece piece) {
        if (!piece.isIdle()) {
            return false;
        }
        Long restEndTime = restEndTimes.get(piece);
        return restEndTime == null || clock.now() >= restEndTime;
    }

    private void trySelect(Position clicked) {
        board().pieceAt(clicked).ifPresent(piece -> {
            if (isAvailableToAct(piece)) {
                selectedPosition = clicked;
            }
        });
    }

    private void tryActOnSelection(Position clicked) {
        Optional<Piece> selectedPiece = board().pieceAt(selectedPosition);
        if (selectedPiece.isEmpty()) {
            selectedPosition = null;
            return;
        }

        boolean clickedOwnAvailablePiece = board().pieceAt(clicked)
                .map(p -> p.isSameColor(selectedPiece.get()) && isAvailableToAct(p))
                .orElse(false);

        if (clickedOwnAvailablePiece) {
            selectedPosition = clicked;
            return;
        }

        tryMove(selectedPiece.get(), selectedPosition, clicked);
        selectedPosition = null;
    }

    private void tryMove(Piece piece, Position from, Position to) {
        if (!isAvailableToAct(piece)) {
            return;
        }
        if (!ruleEngine.isLegalMove(board(), piece, from, to)) {
            return;
        }

        long startTime = clock.now();
        long arrivalTime = startTime + travelTimeFor(from, to);
        piece.markInTransit();
        activeMotions.add(new Motion(piece, from, to, startTime, arrivalTime));
    }

    private long travelTimeFor(Position from, Position to) {
        int distance = Math.max(Math.abs(to.row() - from.row()), Math.abs(to.col() - from.col()));
        return distance * MILLISECONDS_PER_SQUARE;
    }

    private void advanceGameState() {
        // הסדר כאן קריטי: אם כלי מגן מסיים קפיצה בדיוק באותה מילישנייה
        // שבה כלי אחר מגיע אליו, הוא עדיין נחשב "באוויר" באותו טיק -
        // ולכן צריך לפתור הגעות מול מצב הקפיצה הישן, ורק אחר-כך לפוג
        // את הקפיצה עבור הטיק הבא.
        resolveArrivedMotions();
        resolveExpiredJumps();
        purgeExpiredCaptureEffects();
        purgeExpiredRestEntries();
    }

    /** מנקה רשומות מנוחה שכבר פקעו, כדי שהמפה לא תגדל ללא גבול לאורך משחק ארוך. */
    private void purgeExpiredRestEntries() {
        restEndTimes.entrySet().removeIf(entry -> clock.now() >= entry.getValue());
    }

    /** מנקה אפקטי לכידה שכבר עברו את משך החיים שלהם (ר' CAPTURE_EFFECT_DURATION_MS). */
    private void purgeExpiredCaptureEffects() {
        recentCaptures.removeIf(effect -> clock.now() - effect.removedAt() >= CAPTURE_EFFECT_DURATION_MS);
    }

    /** רושם שכלי הוסר הרגע מהלוח, לצורך אפקט הלכידה הקצר ב-UI. */
    private void registerCaptureEffect(Piece removedPiece, Position at) {
        recentCaptures.add(new CaptureEffect(removedPiece.kind(), removedPiece.color(), at, clock.now()));
    }

    private void resolveExpiredJumps() {
        List<Piece> finishedJumpers = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : jumpEndTimes.entrySet()) {
            if (clock.now() >= entry.getValue()) {
                finishedJumpers.add(entry.getKey());
            }
        }
        for (Piece piece : finishedJumpers) {
            piece.markJumpEnded();
            jumpEndTimes.remove(piece);
            jumpStartTimes.remove(piece);
            restEndTimes.put(piece, clock.now() + LONG_REST_DURATION_MS);
        }
    }

    private void resolveArrivedMotions() {
        List<Motion> arrivedMotions = new ArrayList<>();
        for (Motion motion : activeMotions) {
            if (motion.hasArrived(clock.now())) {
                arrivedMotions.add(motion);
            }
        }
        for (Motion motion : arrivedMotions) {
            completeMotion(motion);
        }
        activeMotions.removeAll(arrivedMotions);
    }

    private void completeMotion(Motion motion) {
        Piece movingPiece = motion.piece();

        // אם המשבצת שממנה יצא הכלי הזה כבר לא מחזיקה אותו (זהות, לא רק
        // סוג/צבע) - סימן שכלי אחר "עבר דרכו" באותו טיק וכבר תפס אותו.
        // המהלך הזה מת ואין מה להשלים בו.
        Optional<Piece> pieceStillAtOrigin = board().pieceAt(motion.from());
        if (pieceStillAtOrigin.isEmpty() || pieceStillAtOrigin.get() != movingPiece) {
            return;
        }

        Optional<Piece> defender = board().pieceAt(motion.to());

        if (defender.isPresent() && defender.get().isJumping()) {
            captureFailsAgainstJumpingDefender(motion, movingPiece);
            return;
        }

        checkForKingCapture(movingPiece, defender);
        defender.ifPresent(captured -> registerCaptureEffect(captured, motion.to()));
        recordMove(movingPiece, motion.from(), motion.to(), defender.orElse(null));
        board().movePieceTo(motion.from(), motion.to());
        movingPiece.markArrived();
        restEndTimes.put(movingPiece, clock.now() + SHORT_REST_DURATION_MS);
        maybePromote(movingPiece, motion.to());
    }

    /**
     * "לכידה באוויר": אם כלי מגן נמצא במצב קפיצה במשבצת היעד, הכלי
     * התוקף "מתאדה" (נעלם מהמקור) והמגן נשאר מוגן במקומו.
     */
    private void captureFailsAgainstJumpingDefender(Motion motion, Piece movingPiece) {
        recordFailedCapture(movingPiece, motion.from(), motion.to());
        registerCaptureEffect(movingPiece, motion.from());
        board().removePieceAt(motion.from());
        movingPiece.markArrived();
    }

    /**
     * מעדכן ניקוד (אם הייתה לכידה) ומוסיף שורה לרשימת המהלכים של הצבע
     * שביצע את המהלך - נקרא *לפני* שהלוח מתעדכן בפועל, כדי שעדיין
     * אפשר לדעת מי היה במשבצת היעד.
     */
    private void recordMove(Piece movingPiece, Position from, Position to, Piece captured) {
        boolean isCapture = captured != null;
        if (isCapture) {
            scores.merge(movingPiece.color(), captured.kind().value(), Integer::sum);
        }
        String notation = movingPiece.kind().code() + squareName(from)
                + (isCapture ? "x" : "-") + squareName(to);
        moveLog.get(movingPiece.color()).add(notation);
    }

    /** מהלך תקיפה שנכשל מול כלי קופץ - מתועד ברשימת המהלכים בלי שינוי ניקוד. */
    private void recordFailedCapture(Piece movingPiece, Position from, Position to) {
        String notation = movingPiece.kind().code() + squareName(from) + "x" + squareName(to) + "?!";
        moveLog.get(movingPiece.color()).add(notation);
    }

    /** ממיר Position לסימון שח-מטי מוכר (עמודה a.. + שורה ממוספרת מלמטה). */
    private String squareName(Position pos) {
        char file = (char) ('a' + pos.col());
        int rank = board().height() - pos.row();
        return "" + file + rank;
    }

    private void checkForKingCapture(Piece movingPiece, Optional<Piece> defender) {
        defender.ifPresent(captured -> {
            if (captured.kind() == PieceKind.KING) {
                // מי שלכד את המלך (לא המלך שנלכד) הוא המנצח.
                game.markGameOver(movingPiece.color());
            }
        });
    }

    private void maybePromote(Piece piece, Position at) {
        if (piece.kind() != PieceKind.PAWN) {
            return;
        }
        boolean reachedLastRow = (piece.color() == PieceColor.WHITE && at.row() == 0)
                || (piece.color() == PieceColor.BLACK && at.row() == board().height() - 1);
        if (reachedLastRow) {
            board().replacePieceAt(at, new Piece(piece.color(), PieceKind.QUEEN));
        }
    }

    public Optional<Position> selectedPosition() {
        return Optional.ofNullable(selectedPosition);
    }

    /** עותק הגנתי: ניקוד נוכחי לכל צבע (למשל להצגה בפאנל הצד). */
    public Map<PieceColor, Integer> scores() {
        return Map.copyOf(scores);
    }

    /** עותק הגנתי: רשימת המהלכים (בסימון שח-מטי) שביצע כל צבע עד כה. */
    public Map<PieceColor, List<String>> moveLog() {
        Map<PieceColor, List<String>> copy = new EnumMap<>(PieceColor.class);
        for (Map.Entry<PieceColor, List<String>> entry : moveLog.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }
    public long now() {
    return clock.now();
}

    /**
     * עותק הגנתי של המהלכים הפעילים כרגע - נחוץ לשכבת ה-UI כדי לצייר
     * את הכלי "הולך" בהדרגה בין המשבצות (אינטרפולציה), במקום "לקפוץ"
     * ישר ליעד ברגע שהמהלך מסתיים.
     */
    public List<Motion> activeMotions() {
        return List.copyOf(activeMotions);
    }

    /**
     * כל הקפיצות הפעילות כרגע, עם זמני התחלה/סיום - נחוץ לשכבת ה-UI
     * כדי לצייר קשת גובה (הכלי "עולה" ו"יורד") בזמן הקפיצה, בדיוק כמו
     * ש-activeMotions() משמש לצייר הליכה הדרגתית בין משבצות.
     */
    public List<JumpVisual> activeJumps() {
        List<JumpVisual> jumps = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : jumpEndTimes.entrySet()) {
            Piece piece = entry.getKey();
            long endTime = entry.getValue();
            long startTime = jumpStartTimes.getOrDefault(piece, endTime - DEFAULT_JUMP_DURATION_MS);
            jumps.add(new JumpVisual(piece, startTime, endTime));
        }
        return jumps;
    }

    /** עותק הגנתי של אפקטי הלכידה הפעילים כרגע (ר' CaptureEffect). */
    public List<CaptureEffect> recentCaptureEffects() {
        return List.copyOf(recentCaptures);
    }

    /**
     * כל המשבצות שהכלי שנמצא ב-from יכול לזוז אליהן חוקית *כרגע*
     * (משמש את שכבת ה-UI כדי להאיר את המשבצות האפשריות אחרי לחיצה על כלי).
     * מחזיר רשימה ריקה אם אין כלי במשבצת, או שהכלי לא IDLE.
     */
    public List<Position> legalMovesFrom(Position from) {
        List<Position> moves = new ArrayList<>();
        if (from == null) {
            return moves;
        }
        Optional<Piece> pieceOpt = board().pieceAt(from);
        if (pieceOpt.isEmpty() || !isAvailableToAct(pieceOpt.get())) {
            return moves;
        }
        Piece piece = pieceOpt.get();
        for (int row = 0; row < board().height(); row++) {
            for (int col = 0; col < board().width(); col++) {
                Position to = new Position(row, col);
                if (ruleEngine.isLegalMove(board(), piece, from, to)) {
                    moves.add(to);
                }
            }
        }
        return moves;
    }
}