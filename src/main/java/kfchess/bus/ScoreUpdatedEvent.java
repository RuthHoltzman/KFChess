package kfchess.bus;

import kfchess.model.PieceColor;

/** מתפרסם כל פעם שהניקוד של צבע מסוים משתנה (אחרי לכידה). */
public record ScoreUpdatedEvent(PieceColor color, int newScore) implements GameEvent {
}