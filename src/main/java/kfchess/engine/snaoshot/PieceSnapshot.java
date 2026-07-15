package kfchess.engine.snaoshot;

import kfchess.engine.PieceVisualState;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;

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
