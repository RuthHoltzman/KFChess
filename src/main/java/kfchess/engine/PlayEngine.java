package kfchess.engine;

import kfchess.model.Board;
import kfchess.model.PlayState;
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
import kfchess.bus.PlayLifecycleEvent;
import kfchess.engine.snapshot.CaptureEffect;
import kfchess.engine.snapshot.CaptureEffectTracker;
import kfchess.engine.snapshot.JumpVisual;


/** Drives the game on the timeline: applies rules, advances motions/timers, and publishes bus events. */
public class PlayEngine {

    private static final long MILLISECONDS_PER_SQUARE = 1000;

    private final PlayState game;
    private final RuleEngine ruleEngine;
    private final RaelTime clock;
    private final EventBus bus;
    private final List<Motion> activeMotions = new ArrayList<>();
    private final PieceTimers pieceTimers = new PieceTimers();
    private final CaptureEffectTracker captureEffects = new CaptureEffectTracker();

    private final Map<Piece, Position> chainFinalTarget = new HashMap<>();
    private final Map<Piece, Position> chainOriginalFrom = new HashMap<>();

    private final MoveHistory history;

    public PlayEngine(PlayState game, RuleEngine ruleEngine, RaelTime clock, EventBus bus) {
        this.game = game;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
        this.bus = bus;
        this.history = new MoveHistory(game.board(), bus);
    }

    public boolean isGameOver() {
        return game.isGameOver();
    }

    public Optional<PieceColor> winner() {
        return game.winner();
    }

    public Board board() {
        return game.board();
    }

    /** Advances the clock by the given elapsed time, then resolves whatever that completes. */
    public void handleWait(long milliseconds) {
        if (game.isGameOver()) {
            return;
        }
        clock.advance(milliseconds);
        advanceGameState();
    }

    /** Package-private: lets PlayCommandController start a jump without duplicating PieceTimers access. */
    void beginJump(Piece piece) {
        pieceTimers.beginJump(piece, clock.now());
    }

    /** Package-private: whether a piece is idle and past its rest cooldown. */
    boolean isAvailableToAct(Piece piece) {
        return pieceTimers.isAvailableToAct(piece, clock.now());
    }

    /** Package-private: validates and starts a move as a Motion, chaining multi-square slides one hop at a time. */
    void tryMove(Piece piece, Position from, Position to) {
        if (!isAvailableToAct(piece)) {
            return;
        }
        if (!ruleEngine.isLegalMove(board(), piece, from, to)) {
            return;
        }

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

    /** Milliseconds a piece needs to cross the given (single-hop) distance. */
    private long travelTimeFor(Position from, Position to) {
        int distance = Math.max(Math.abs(to.row() - from.row()), Math.abs(to.col() - from.col()));
        return distance * MILLISECONDS_PER_SQUARE;
    }

    /** Package-private: resolves everything time-based (arrivals, jump expiry, effect/rest cleanup) for "now". */
    void advanceGameState() {
        resolveArrivedMotions();
        pieceTimers.resolveExpiredJumps(clock.now());
        captureEffects.purgeExpired(clock.now());
        pieceTimers.purgeExpiredRest(clock.now());
    }

    /** Finds every Motion that has reached its destination and completes it. */
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

    /** Applies the effect of one finished Motion: capture, block, chained slide, or a failed capture mid-jump. */
    private void completeMotion(Motion motion) {
        Piece movingPiece = motion.piece();


        Optional<Piece> pieceStillAtOrigin = board().pieceAt(motion.from());
        if (pieceStillAtOrigin.isEmpty() || pieceStillAtOrigin.get() != movingPiece) {
            return;
        }

        Optional<Piece> defender = board().pieceAt(motion.to());

        if (defender.isPresent() && defender.get().isJumping()) {
            captureFailsAgainstJumpingDefender(motion, movingPiece, defender.get());
            chainFinalTarget.remove(movingPiece);
            chainOriginalFrom.remove(movingPiece);
            return;
        }

        if (defender.isPresent() && defender.get().isSameColor(movingPiece)) {

            history.recordBlockedMove(movingPiece, chainOriginalFrom.getOrDefault(movingPiece, motion.from()), motion.to());
            movingPiece.markArrived();
            pieceTimers.beginShortRest(movingPiece, clock.now());
            chainFinalTarget.remove(movingPiece);
            chainOriginalFrom.remove(movingPiece);
            return;
        }

        Position finalTarget = chainFinalTarget.get(movingPiece);

        boolean chainContinues = defender.isEmpty() && finalTarget != null && !finalTarget.equals(motion.to());

        if (chainContinues) {

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


    /** Handles an attacker landing on a piece that's mid-jump: attacker vanishes, defender scores and survives. */
    private void captureFailsAgainstJumpingDefender(Motion motion, Piece movingPiece, Piece defendingPiece) {
        history.recordCounterCapture(defendingPiece, movingPiece, motion.from(), motion.to());
        captureEffects.register(movingPiece, motion.from(), clock.now());
        board().removePieceAt(motion.from());
        movingPiece.markArrived();
    }

    /** Ends the game if the captured piece was a king. */
    private void checkForKingCapture(Piece movingPiece, Optional<Piece> defender) {
        defender.ifPresent(captured -> {
            if (captured.kind() == PieceKind.KING) {
                forceGameOver(movingPiece.color());
            }
        });
    }

    /** Ends the game immediately for the given winner - shared by king capture, disconnect timeout, etc. */
    public void forceGameOver(PieceColor winner) {
        game.markGameOver(winner);
        bus.publish(new PlayLifecycleEvent(PlayLifecycleEvent.Phase.ENDED, winner));
    }

    /** Promotes a pawn to a queen if it just reached the last row. */
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

    public Map<PieceColor, Integer> scores() {
        return history.scores();
    }

    public Map<PieceColor, List<String>> moveLog() {
        return history.moveLog();
    }

    public long now() {
        return clock.now();
    }

    /** Snapshot of all pieces currently sliding between squares. */
    public List<Motion> activeMotions() {
        return List.copyOf(activeMotions);
    }

    /** Snapshot of all pieces currently mid-jump. */
    public List<JumpVisual> activeJumps() {
        return pieceTimers.activeJumps();
    }

    /** Recent captures still worth drawing a visual effect for. */
    public List<CaptureEffect> recentCaptureEffects() {
        return captureEffects.active();
    }

    /** Every board position a piece at "from" could legally move to right now. */
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