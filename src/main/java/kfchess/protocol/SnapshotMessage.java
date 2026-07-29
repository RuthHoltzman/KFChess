package kfchess.protocol;

import kfchess.engine.snapshot.CaptureEffect;
import kfchess.model.Position;
import kfchess.realtime.Motion;

import java.util.List;
import java.util.Map;


/** Full board state, broadcast to every connection each tick; "selected"/"legalMoves" are per-recipient. */
public class SnapshotMessage {

    private final String type = "SNAPSHOT";
    private final int boardWidthCells;
    private final int boardHeightCells;
    private final List<PieceDto> pieces;
    private final Position selected;
    private final List<Position> legalMoves;
    private final Map<String, Integer> scores;
    private final Map<String, List<String>> moveLog;
    private final boolean gameOver;
    private final String winner;
    private final long now;
    private final List<Motion> motions;
    private final List<JumpDto> jumps;
    private final List<CaptureEffect> captureEffects;

    // Only meaningful when gameOver: this recipient already voted RESTART and is waiting for the other side.
    private final boolean restartRequestedByViewer;

    // Seconds until a disconnected player forfeits; null (not 0) when no grace window is active.
    private final Integer disconnectSecondsRemaining;

    // True while only one player is connected, so the client can draw a "Waiting for an opponent..." banner.
    private final boolean waitingForOpponent;

    /** Convenience overload: no restart vote, no disconnect countdown, not waiting for an opponent. */
    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects) {
        this(boardWidthCells, boardHeightCells, pieces, selected, legalMoves, scores, moveLog, gameOver, winner, now,
                motions, jumps, captureEffects, false, null);
    }

    /** Convenience overload: not waiting for an opponent. */
    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects,
                           boolean restartRequestedByViewer, Integer disconnectSecondsRemaining) {
        this(boardWidthCells, boardHeightCells, pieces, selected, legalMoves, scores, moveLog, gameOver, winner, now,
                motions, jumps, captureEffects, restartRequestedByViewer, disconnectSecondsRemaining, false);
    }

    /** Full constructor - the only one production code uses. */
    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects,
                           boolean restartRequestedByViewer, Integer disconnectSecondsRemaining,
                           boolean waitingForOpponent) {
        this.boardWidthCells = boardWidthCells;
        this.boardHeightCells = boardHeightCells;
        this.pieces = pieces;
        this.selected = selected;
        this.legalMoves = legalMoves;
        this.scores = scores;
        this.moveLog = moveLog;
        this.gameOver = gameOver;
        this.winner = winner;
        this.now = now;
        this.motions = motions;
        this.jumps = jumps;
        this.captureEffects = captureEffects;
        this.restartRequestedByViewer = restartRequestedByViewer;
        this.disconnectSecondsRemaining = disconnectSecondsRemaining;
        this.waitingForOpponent = waitingForOpponent;
    }
}
