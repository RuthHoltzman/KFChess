package kfchess.bus;

import kfchess.model.PieceColor;

/** מתפרסם כל פעם שנוספת שורה ליומן המהלכים של צבע מסוים. */
public record MoveLoggedEvent(PieceColor color, String notation) implements GameEvent {
}