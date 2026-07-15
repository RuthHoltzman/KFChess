package kfchess.engine;

import kfchess.model.Piece;
import kfchess.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SnapshotFactory {

    private final PieceVisualStateTracker visualStateTracker = new PieceVisualStateTracker();
    private final int cellWidth;
    private final int cellHeight;
    private final int rows;
    private final int cols;

    public SnapshotFactory(int cellWidth, int cellHeight, int rows, int cols) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.rows = rows;
        this.cols = cols;
    }

    public GameSnapshot createSnapshot(
            Map<Position, Piece> pieces,
            long now,
            Position selectedPosition,
            boolean gameOver,
            String winner
    ) {
        List<PieceSnapshot> pieceSnapshots = new ArrayList<>();

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Position pos = entry.getKey();
            Piece piece = entry.getValue();

            PieceVisualState visualState = visualStateTracker.resolve(piece, now);
            long stateElapsed = visualStateTracker.elapsedInCurrentVisualState(piece, now);

            double pixelX = pos.col() * cellWidth;
            double pixelY = pos.row() * cellHeight;

            String id = "" + piece.color().code() + piece.kind().code() + "@" + pos;

            pieceSnapshots.add(new PieceSnapshot(
                    id, piece.kind(), piece.color(), visualState,
                    pixelX, pixelY, stateElapsed
            ));
        }

        return new GameSnapshot(cols, rows, pieceSnapshots, selectedPosition, gameOver, winner);
    }
}