package kfchess.engine.snapshot;

import kfchess.model.Piece;


/** A piece currently mid-jump, with the timestamps needed to compute its arc animation. */
public record JumpVisual(Piece piece, long startTime, long endTime) {}
