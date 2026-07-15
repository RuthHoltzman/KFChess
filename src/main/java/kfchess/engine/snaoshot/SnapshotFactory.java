package kfchess.engine.snaoshot;

import kfchess.engine.PieceVisualState;
import kfchess.engine.PieceVisualStateTracker;
import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.realtime.Motion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotFactory {

    private final PieceVisualStateTracker visualStateTracker = new PieceVisualStateTracker();
    private final int cellWidth;
    private final int cellHeight;

    public SnapshotFactory(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public GameSnapshot createSnapshot(
            Board board,
            long now,
            Position selectedPosition,
            boolean gameOver,
            String winner,
            List<Motion> activeMotions,
            List<Position> legalMoves,
            Map<PieceColor, Integer> scores,
            Map<PieceColor, List<String>> moveLog
    ) {
        // מיפוי כלי -> Motion פעיל, כדי לדעת עבור כל כלי אם הוא "בדרך"
        // כרגע ולחשב עבורו מיקום פיקסלים מתקדם (הליכה) ולא רק את המשבצת
        // המקורית (מה שהיה נראה כמו "קפיצה" ליעד ברגע שהמהלך הסתיים).
        Map<Piece, Motion> motionByPiece = new HashMap<>();
        for (Motion motion : activeMotions) {
            motionByPiece.put(motion.piece(), motion);
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
                    }

                    String id = "" + piece.color().code() + piece.kind().code() + "@" + pos;

                    pieceSnapshots.add(new PieceSnapshot(
                            id, piece.kind(), piece.color(), visualState,
                            pixelX, pixelY, stateElapsed, restProgress
                    ));
                });
            }
        }

        return new GameSnapshot(board.width(), board.height(), pieceSnapshots, selectedPosition, legalMoves,
                gameOver, winner, scores, moveLog);
    }
}
