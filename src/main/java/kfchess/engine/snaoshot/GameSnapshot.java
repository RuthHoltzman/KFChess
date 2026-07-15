package kfchess.engine.snaoshot;

import kfchess.model.PieceColor;
import kfchess.model.Position;
import java.util.List;
import java.util.Map;

public record GameSnapshot(
        int boardWidthCells,
        int boardHeightCells,
        List<PieceSnapshot> pieces,
        Position selectedPosition,
        List<Position> legalMoves,
        boolean gameOver,
        String winner,
        Map<PieceColor, Integer> scores,
        Map<PieceColor, List<String>> moveLog
) {
    public GameSnapshot {
        pieces = List.copyOf(pieces); // הגנה - אי אפשר לשנות את הרשימה אחרי היצירה
        legalMoves = List.copyOf(legalMoves);
        scores = Map.copyOf(scores);
        moveLog = Map.copyOf(moveLog);
    }
}
