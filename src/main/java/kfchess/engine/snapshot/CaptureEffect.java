package kfchess.engine.snapshot;

import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;


/** A captured piece still fading out visually, tracked by when it was removed. */
public record CaptureEffect(PieceKind kind, PieceColor color, Position at, long removedAt) {}
