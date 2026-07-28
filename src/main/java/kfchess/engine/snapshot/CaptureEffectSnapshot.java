package kfchess.engine.snapshot;

import kfchess.model.PieceColor;
import kfchess.model.PieceKind;


/** Wire/DTO form of a CaptureEffect: pixel position and fade progress instead of raw timestamps. */
public record CaptureEffectSnapshot(
        PieceKind kind,
        PieceColor color,
        double pixelX,
        double pixelY,
        double progress
) {}
