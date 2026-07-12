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

    private final Game game;
    private final RuleEngine ruleEngine;
    private final RaelTime clock;

    private final List<Motion> activeMotions = new ArrayList<>();
    private final Map<Piece, Long> jumpEndTimes = new HashMap<>();
    private Position selectedPosition;

    public GameEngine(Game game, RuleEngine ruleEngine, RaelTime clock) {
        this.game = game;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
    }

    public boolean isGameOver() {
        return game.isGameOver();
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
        clock.advance(milliseconds);
        advanceGameState();
    }

    public void handleJump(Position target) {
        board().pieceAt(target).ifPresent(piece -> {
            if (piece.isIdle()) {
                piece.markJumping();
                jumpEndTimes.put(piece, clock.now() + DEFAULT_JUMP_DURATION_MS);
            }
        });
    }

    private void trySelect(Position clicked) {
        board().pieceAt(clicked).ifPresent(piece -> {
            if (piece.isIdle()) {
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

        boolean clickedOwnPiece = board().pieceAt(clicked)
                .map(p -> p.isSameColor(selectedPiece.get()))
                .orElse(false);

        if (clickedOwnPiece) {
            selectedPosition = clicked;
            return;
        }

        tryMove(selectedPiece.get(), selectedPosition, clicked);
        selectedPosition = null;
    }

    private void tryMove(Piece piece, Position from, Position to) {
        if (!piece.isIdle()) {
            return;
        }
        if (!ruleEngine.isLegalMove(board(), piece, from, to)) {
            return;
        }

        long arrivalTime = clock.now() + travelTimeFor(from, to);
        piece.markInTransit();
        activeMotions.add(new Motion(piece, from, to, arrivalTime));
    }

    private long travelTimeFor(Position from, Position to) {
        int distance = Math.max(Math.abs(to.row() - from.row()), Math.abs(to.col() - from.col()));
        return distance * MILLISECONDS_PER_SQUARE;
    }

    private void advanceGameState() {
        resolveExpiredJumps();
        resolveArrivedMotions();
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
        Optional<Piece> defender = board().pieceAt(motion.to());

        if (defender.isPresent() && defender.get().isJumping()) {
            captureFailsAgainstJumpingDefender(motion, movingPiece);
            return;
        }

        checkForKingCapture(defender);
        board().movePieceTo(motion.from(), motion.to());
        movingPiece.markArrived();
        maybePromote(movingPiece, motion.to());
    }

    /**
     * "לכידה באוויר": אם כלי מגן נמצא במצב קפיצה במשבצת היעד, הכלי
     * התוקף "מתאדה" (נעלם מהמקור) והמגן נשאר מוגן במקומו.
     */
    private void captureFailsAgainstJumpingDefender(Motion motion, Piece movingPiece) {
        board().removePieceAt(motion.from());
        movingPiece.markArrived();
    }

    private void checkForKingCapture(Optional<Piece> defender) {
        defender.ifPresent(captured -> {
            if (captured.kind() == PieceKind.KING) {
                game.markGameOver();
            }
        });
    }

    private void maybePromote(Piece piece, Position at) {
        if (piece.kind() != PieceKind.PAWN) {
            return;
        }
        boolean reachedLastRow =
                (piece.color() == PieceColor.WHITE && at.row() == 0)
                        || (piece.color() == PieceColor.BLACK && at.row() == board().height() - 1);
        if (reachedLastRow) {
            board().replacePieceAt(at, new Piece(piece.color(), PieceKind.QUEEN));
        }
    }
}
