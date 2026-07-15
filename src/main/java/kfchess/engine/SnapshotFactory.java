package kfchess.engine;

import kfchess.model.Board;
import kfchess.model.Position;

import java.util.ArrayList;
import java.util.List;

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
            String winner
    ) {
        List<PieceSnapshot> pieceSnapshots = new ArrayList<>();

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Position pos = new Position(row, col);
                board.pieceAt(pos).ifPresent(piece -> {
                    PieceVisualState visualState = visualStateTracker.resolve(piece, now);
                    long stateElapsed = visualStateTracker.elapsedInCurrentVisualState(piece, now);

                    double pixelX = pos.col() * cellWidth;
                    double pixelY = pos.row() * cellHeight;
                    String id = "" + piece.color().code() + piece.kind().code() + "@" + pos;

                    pieceSnapshots.add(new PieceSnapshot(
                            id, piece.kind(), piece.color(), visualState,
                            pixelX, pixelY, stateElapsed
                    ));
                });
            }
        }

        return new GameSnapshot(board.width(), board.height(), pieceSnapshots, selectedPosition, gameOver, winner);
    }
}