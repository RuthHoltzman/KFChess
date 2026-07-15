package kfchess.engine;

import kfchess.model.Position;
import java.util.List;

public record GameSnapshot(
        int boardWidthCells,
        int boardHeightCells,
        List<PieceSnapshot> pieces,
        Position selectedPosition,
        boolean gameOver,
        String winner
) {
    public GameSnapshot {
        pieces = List.copyOf(pieces); // הגנה - אי אפשר לשנות את הרשימה אחרי היצירה
    }
}