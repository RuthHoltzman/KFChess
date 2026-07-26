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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import kfchess.bus.EventBus;
import kfchess.bus.GameLifecycleEvent;
import kfchess.engine.snapshot.CaptureEffect;
import kfchess.engine.snapshot.CaptureEffectTracker;
import kfchess.engine.snapshot.JumpVisual;

/**
 * "המוח" של המשחק: מקבל אירועים ברמת התחום (קליק על תא, המתנה, קפיצה)
 * ומתרגם אותם לשינויים ב-Game/Board, תוך אכיפת חוקי התנועה דרך RuleEngine.
 * <p>
 * שימו לב: הכלי הנבחר (selection), רשימת המהלכים הפעילים (activeMotions)
 * וזמני הקפיצה (ר' PieceTimers) הן state per-engine ולא סטטי
 * גלובלי - כל אלה מאפשרים בעתיד להריץ כמה משחקים/לוחות במקביל, וגם
 * מאפשרים לכמה כלים לזוז/לקפוץ בו-זמנית (בניגוד לקוד המקורי שתמך
 * בכלי אחד בתנועה ובקפיצה אחת בלבד באמצעות משתנים סטטיים).
 */
public class GameEngine {

    private static final long MILLISECONDS_PER_SQUARE = 1000;

    private final Game game;
    private final RuleEngine ruleEngine;
    private final RaelTime clock;
    private final EventBus bus;
    private final List<Motion> activeMotions = new ArrayList<>();
    // "כמה זמן כלי נעול/קופץ/במנוחה" - הוצא למחלקה נפרדת, ר' PieceTimers.
    private final PieceTimers pieceTimers = new PieceTimers();
    // אפקטי לכידה זמניים (לאנימציית דהייה ב-UI בלבד) - הוצא למחלקה
    // נפרדת, ר' CaptureEffectTracker.
    private final CaptureEffectTracker captureEffects = new CaptureEffectTracker();
    // תנועה ארוכה (למשל צריח e1->e8) מפורקת עכשיו לשרשרת של קפיצות של
    // משבצת אחת - כל קפיצה מעדכנת את הלוח מיד כשהיא מסתיימת, וכך אפשר
    // לבדוק בזמן אמת אם משבצת הביניים תפוסה. chainFinalTarget הוא היעד
    // ה*סופי* של השרשרת (e8), לעומת motion.to() שהוא רק הקפיצה הנוכחית.
    // chainOriginalFrom הוא המשבצת שממנה התחילה השרשרת כולה (e1), רק
    // כדי שיומן המהלכים ירשום שורה אחת נקייה במקום שורה לכל משבצת.
    // סוס אף פעם לא מקבל entry כאן - אין לו "משבצת ביניים" הגיונית.
    private final Map<Piece, Position> chainFinalTarget = new HashMap<>();
    private final Map<Piece, Position> chainOriginalFrom = new HashMap<>();
    private Position selectedPosition;

    // ניקוד + יומן מהלכים לכל צבע - הוצא למחלקה נפרדת, ר' MoveHistory.
    private final MoveHistory history;

    public GameEngine(Game game, RuleEngine ruleEngine, RaelTime clock, EventBus bus) {
        this.game = game;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
        this.bus = bus;
        this.history = new MoveHistory(game.board(), bus);
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
                pieceTimers.beginJump(piece, clock.now());
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

    // שתי המתודות הבאות package-private (לא private) כי NetworkActions
    // צריך בדיוק אותה התנהגות בשביל המשחק הרשתי - בלי לשכפל אותה, ובלי
    // ש-NetworkActions יצטרך להכיר את PieceTimers בכלל (הוא מכיר רק
    // את GameEngine - ר' NetworkActions.java).

    /** מתחיל קפיצה עבור כלי: מסמן אותו כ-JUMPING ורושם את זמני ההתחלה/סיום. */
    void beginJump(Piece piece) {
        pieceTimers.beginJump(piece, clock.now());
    }

    /**
     * האם כלי זמין כרגע לפעולה (בחירה/תנועה/קפיצה): לא רק "IDLE" ברמת
     * המודל, אלא גם לא נמצא כרגע ב"מנוחה" (cooldown) אחרי הליכה/קפיצה
     * קודמת. זה מה שהופך את שעון החול הצהוב מקישוט בלבד לכלל משחק אמיתי.
     */
    boolean isAvailableToAct(Piece piece) {
        return pieceTimers.isAvailableToAct(piece, clock.now());
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

    // package-private (לא private) כדי ש-NetworkActions יוכל לבצע מהלך
    // אחרי שהוא כבר וידא בעלות/זמינות - אותה בדיוק לוגיקת חוקיות/שרשור.
    void tryMove(Piece piece, Position from, Position to) {
        if (!isAvailableToAct(piece)) {
            return;
        }
        if (!ruleEngine.isLegalMove(board(), piece, from, to)) {
            return;
        }

        // תנועה "ניתנת לשרשור" = קו ישר או אלכסון (חייל/צריח/רץ/מלכה/מלך).
        // לסוס אין משבצת ביניים הגיונית (התבנית שלו (2,1) לא ליניארית),
        // ולכן הוא תמיד ממשיך כקפיצה ישירה אחת - בדיוק כמו היום.
        boolean isSlidingMove = to.row() == from.row() || to.col() == from.col()
                || Math.abs(to.row() - from.row()) == Math.abs(to.col() - from.col());
        boolean isMultiSquare = Math.max(Math.abs(to.row() - from.row()), Math.abs(to.col() - from.col())) > 1;

        Position nextHop = to;
        if (isSlidingMove && isMultiSquare) {
            int rowStep = Integer.signum(to.row() - from.row());
            int colStep = Integer.signum(to.col() - from.col());
            nextHop = new Position(from.row() + rowStep, from.col() + colStep);
            chainFinalTarget.put(piece, to);
            chainOriginalFrom.put(piece, from);
        }

        long startTime = clock.now();
        long arrivalTime = startTime + travelTimeFor(from, nextHop);
        piece.markInTransit();
        activeMotions.add(new Motion(piece, from, nextHop, startTime, arrivalTime));
    }

    private long travelTimeFor(Position from, Position to) {
        int distance = Math.max(Math.abs(to.row() - from.row()), Math.abs(to.col() - from.col()));
        return distance * MILLISECONDS_PER_SQUARE;
    }

    // package-private כדי ש-NetworkActions יוכל "לקדם" את שעון המשחק
    // לפני שהוא מטפל בקליק, בדיוק כמו handleClick/handleWait/handleJump.
    void advanceGameState() {
        // הסדר כאן קריטי: אם כלי מגן מסיים קפיצה בדיוק באותה מילישנייה
        // שבה כלי אחר מגיע אליו, הוא עדיין נחשב "באוויר" באותו טיק -
        // ולכן צריך לפתור הגעות מול מצב הקפיצה הישן, ורק אחר-כך לפוג
        // את הקפיצה עבור הטיק הבא.
        resolveArrivedMotions();
        pieceTimers.resolveExpiredJumps(clock.now());
        captureEffects.purgeExpired(clock.now());
        pieceTimers.purgeExpiredRest(clock.now());
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
            chainFinalTarget.remove(movingPiece);
            chainOriginalFrom.remove(movingPiece);
            return;
        }

        if (defender.isPresent() && defender.get().isSameColor(movingPiece)) {
            // "כמעט התנגשות" עם כלי ידידותי: הכלי הנוסע לא נכנס למשבצת
            // הזו בכלל ונשאר בדיוק במשבצת שממנה יצא לקפיצה הזו - שהיא,
            // בדיוק בזכות פירוק התנועה למשבצת-משבצת, "המשבצת הקודמת"
            // המבוקשת. השרשרת נגמרת כאן, בלי לכידה ובלי עדכון לוח.
            history.recordBlockedMove(movingPiece, chainOriginalFrom.getOrDefault(movingPiece, motion.from()), motion.to());
            movingPiece.markArrived();
            pieceTimers.beginShortRest(movingPiece, clock.now());
            chainFinalTarget.remove(movingPiece);
            chainOriginalFrom.remove(movingPiece);
            return;
        }

        Position finalTarget = chainFinalTarget.get(movingPiece);
        // השרשרת ממשיכה רק אם המשבצת ריקה *וגם* עוד לא הגענו ליעד הסופי.
        // אם היה כלי אויב כאן (defender.isPresent()) - הלכידה עוצרת את
        // התנועה כאן ועכשיו, בדיוק כמו בשחמט רגיל (אי אפשר "לעוף" דרך
        // כלי שנלכד ולהמשיך הלאה מעבר לו).
        boolean chainContinues = defender.isEmpty() && finalTarget != null && !finalTarget.equals(motion.to());

        if (chainContinues) {
            // עוד דרך לעבור, והמשבצת ריקה: מזיזים בלוח בלי לדווח על המהלך
            // עדיין (המהלך "האמיתי" מבחינת היומן/הניקוד מסתיים רק כשהשרשרת
            // נגמרת - אחרת כל מהלך ארוך היה מייצר שורה נפרדת ליומן לכל
            // משבצת בדרך). לא נכנסים למנוחה בכלל - חוזרים ל-IN_TRANSIT מיד
            // (שתי הקריאות קורות בו-זמנית, לפני כל רינדור, אז הכלי לעולם
            // לא "נראה" IDLE אפילו לפריים אחד) ומתחילים את קפיצת המשבצת
            // הבאה לכיוון היעד הסופי.
            board().movePieceTo(motion.from(), motion.to());
            movingPiece.markArrived();
            movingPiece.markInTransit();
            int rowStep = Integer.signum(finalTarget.row() - motion.to().row());
            int colStep = Integer.signum(finalTarget.col() - motion.to().col());
            Position nextHop = new Position(motion.to().row() + rowStep, motion.to().col() + colStep);
            long startTime = clock.now();
            activeMotions.add(new Motion(movingPiece, motion.to(), nextHop, startTime,
                    startTime + MILLISECONDS_PER_SQUARE));
            return;
        }

        // כאן השרשרת נגמרת (הגענו ליעד הסופי, או שהיה כלי אויב בדרך ותפסנו
        // אותו) - כאן, ורק כאן, מדווחים על המהלך המלא ליומן/לניקוד.
        checkForKingCapture(movingPiece, defender);
        defender.ifPresent(captured -> captureEffects.register(captured, motion.to(), clock.now()));
        history.recordMove(movingPiece, chainOriginalFrom.getOrDefault(movingPiece, motion.from()), motion.to(),
                defender.orElse(null));
        board().movePieceTo(motion.from(), motion.to());
        movingPiece.markArrived();
        pieceTimers.beginShortRest(movingPiece, clock.now());
        chainFinalTarget.remove(movingPiece);
        chainOriginalFrom.remove(movingPiece);
        maybePromote(movingPiece, motion.to());
    }

    /**
     * "לכידה באוויר": אם כלי מגן נמצא במצב קפיצה במשבצת היעד, הכלי
     * התוקף "מתאדה" (נעלם מהמקור) והמגן נשאר מוגן במקומו.
     */
    private void captureFailsAgainstJumpingDefender(Motion motion, Piece movingPiece) {
        history.recordFailedCapture(movingPiece, motion.from(), motion.to());
        captureEffects.register(movingPiece, motion.from(), clock.now());
        board().removePieceAt(motion.from());
        movingPiece.markArrived();
    }

    private void checkForKingCapture(Piece movingPiece, Optional<Piece> defender) {
        defender.ifPresent(captured -> {
            if (captured.kind() == PieceKind.KING) {
                forceGameOver(movingPiece.color());
            }
        });
    }

    /**
     * מסיימת את המשחק "בכוח" עם מנצח נתון, בלי שום לכידת מלך בפועל -
     * המקום המשותף שגם checkForKingCapture (ניצחון "רגיל") וגם ניתוק/
     * auto-resign (ר' GameSession, שלב 5) קוראים לו, כדי לא לשכפל את
     * שתי הפעולות שחייבות לקרות יחד בכל סיום משחק: לסמן את המצב עצמו
     * כ-game-over (Game.markGameOver) ולפרסם GameLifecycleEvent(ENDED)
     * שממנו GameSession.onGameLifecycleEvent כבר יודע לעדכן ELO -
     * בלי הבדל אם הסיבה לניצחון היא לכידת מלך או ניתוק היריב.
     * לא בודקת isGameOver() בעצמה - זו אחריות הקורא (GameSession כבר
     * בודק !engine.isGameOver() לפני שהיא קוראת לכאן, כדי לא "לדרוס"
     * ניצחון אמיתי שכבר קרה).
     */
    public void forceGameOver(PieceColor winner) {
        game.markGameOver(winner);
        bus.publish(new GameLifecycleEvent(GameLifecycleEvent.Phase.ENDED, winner));
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

    /** ניקוד נוכחי לכל צבע (למשל להצגה בפאנל הצד). */
    public Map<PieceColor, Integer> scores() {
        return history.scores();
    }

    /** רשימת המהלכים (בסימון שח-מטי) שביצע כל צבע עד כה. */
    public Map<PieceColor, List<String>> moveLog() {
        return history.moveLog();
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
        return pieceTimers.activeJumps();
    }

    /** עותק הגנתי של אפקטי הלכידה הפעילים כרגע (ר' CaptureEffect). */
    public List<CaptureEffect> recentCaptureEffects() {
        return captureEffects.active();
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