package kfchess.engine.snapshot;

import kfchess.model.PieceColor;
import kfchess.model.Position;
import java.util.List;
import java.util.Map;

/** Immutable wire/DTO form of the whole game state, sent to the client each tick. */
public record GameSnapshot(
        int boardWidthCells,
        int boardHeightCells,
        List<PieceSnapshot> pieces,
        List<CaptureEffectSnapshot> captureEffects,
        Position selectedPosition,
        List<Position> legalMoves,
        boolean gameOver,
        String winner,
        Map<PieceColor, Integer> scores,
        Map<PieceColor, List<String>> moveLog,
        boolean restartRequestedByViewer,
        Integer disconnectSecondsRemaining,
        boolean waitingForOpponent
) {
    public GameSnapshot {
        pieces = List.copyOf(pieces); // defensive copy - collections can't be mutated after construction
        captureEffects = List.copyOf(captureEffects);
        legalMoves = List.copyOf(legalMoves);
        scores = Map.copyOf(scores);
        moveLog = Map.copyOf(moveLog);
    }
}
