package kfchess.bus;

import kfchess.model.PieceColor;


/** Fired when a move is recorded in the move log, for the given color's notation. */
public record MoveLoggedEvent(PieceColor color, String notation) implements PlayEvent {
}