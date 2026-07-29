package kfchess.engine.snapshot;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.realtime.Motion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds an immutable PlaySnapshot (DTO) from the engine's live, mutable game state. */
public class SnapshotFactory {


    private static final double JUMP_HEIGHT_FRACTION = 0.5;

    private final PieceVisualStateTracker visualStateTracker = new PieceVisualStateTracker();

    /** Assembles a full PlaySnapshot: piece positions/visuals, capture effects, scores, and status flags. */
    public PlaySnapshot createSnapshot(
            Board board,
            int cellWidth,
            int cellHeight,
            long now,
            Position selectedPosition,
            boolean gameOver,
            String winner,
            List<Motion> activeMotions,
            List<JumpVisual> activeJumps,
            List<CaptureEffect> captureEffects,
            List<Position> legalMoves,
            Map<PieceColor, Integer> scores,
            Map<PieceColor, List<String>> moveLog,
            boolean restartRequestedByViewer,
            Integer disconnectSecondsRemaining,
            boolean waitingForOpponent
    ) {

        Map<Piece, Motion> motionByPiece = new HashMap<>();
        for (Motion motion : activeMotions) {
            motionByPiece.put(motion.piece(), motion);
        }


        Map<Piece, JumpVisual> jumpByPiece = new HashMap<>();
        for (JumpVisual jump : activeJumps) {
            jumpByPiece.put(jump.piece(), jump);
        }

        List<PieceSnapshot> pieceSnapshots = new ArrayList<>();

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Position pos = new Position(row, col);
                board.pieceAt(pos).ifPresent(piece -> {
                    PieceVisualState visualState = visualStateTracker.resolve(piece, now);
                    long stateElapsed = visualStateTracker.elapsedInCurrentVisualState(piece, now);
                    double restProgress = visualStateTracker.restProgress(piece, now);

                    double pixelX;
                    double pixelY;
                    Motion motion = motionByPiece.get(piece);
                    if (motion != null) {
                        double progress = motion.progress(now);
                        double fromX = motion.from().col() * cellWidth;
                        double fromY = motion.from().row() * cellHeight;
                        double toX = motion.to().col() * cellWidth;
                        double toY = motion.to().row() * cellHeight;
                        pixelX = fromX + (toX - fromX) * progress;
                        pixelY = fromY + (toY - fromY) * progress;
                    } else {
                        pixelX = pos.col() * cellWidth;
                        pixelY = pos.row() * cellHeight;


                        JumpVisual jump = jumpByPiece.get(piece);
                        if (jump != null) {
                            double progress = progressBetween(jump.startTime(), jump.endTime(), now);
                            double arcHeight = cellHeight * JUMP_HEIGHT_FRACTION;
                            pixelY -= Math.sin(Math.PI * progress) * arcHeight;
                        }
                    }

                    String id = "" + piece.color().code() + piece.kind().code() + "@" + pos;

                    pieceSnapshots.add(new PieceSnapshot(
                            id, piece.kind(), piece.color(), visualState,
                            pixelX, pixelY, stateElapsed, restProgress
                    ));
                });
            }
        }

        List<CaptureEffectSnapshot> captureEffectSnapshots = new ArrayList<>();
        for (CaptureEffect effect : captureEffects) {
            double progress = progressBetween(
                    effect.removedAt(), effect.removedAt() + CaptureEffectTracker.CAPTURE_EFFECT_DURATION_MS, now);
            if (progress >= 1.0) {
                continue; // already fully faded - PlayEngine will purge it next tick, nothing to draw
            }
            double px = effect.at().col() * cellWidth;
            double py = effect.at().row() * cellHeight;
            captureEffectSnapshots.add(new CaptureEffectSnapshot(effect.kind(), effect.color(), px, py, progress));
        }

        return new PlaySnapshot(board.width(), board.height(), pieceSnapshots, captureEffectSnapshots,
                selectedPosition, legalMoves, gameOver, winner, scores, moveLog, restartRequestedByViewer,
                disconnectSecondsRemaining, waitingForOpponent);
    }

    private static double progressBetween(long start, long end, long now) {
        long duration = end - start;
        if (duration <= 0) {
            return 1.0;
        }
        double p = (now - start) / (double) duration;
        if (p < 0) return 0.0;
        if (p > 1) return 1.0;
        return p;
    }
}
