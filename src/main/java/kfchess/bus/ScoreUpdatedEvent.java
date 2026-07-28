package kfchess.bus;

import kfchess.model.PieceColor;


/** Fired whenever a color's score changes (e.g. after a capture). */
public record ScoreUpdatedEvent(PieceColor color, int newScore) implements GameEvent {
}