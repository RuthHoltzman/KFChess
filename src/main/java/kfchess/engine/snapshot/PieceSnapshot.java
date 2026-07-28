package kfchess.engine.snapshot;

import kfchess.model.PieceColor;
import kfchess.model.PieceKind;

/** Wire/DTO form of one piece: pixel position, visual state, and animation progress. */
public record PieceSnapshot(
        String id,
        PieceKind kind,
        PieceColor color,
        PieceVisualState state,
        double pixelX,
        double pixelY,
        long stateElapsedMillis,
        double restProgress
) {}
