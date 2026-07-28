package kfchess.bus;

import kfchess.model.PieceColor;


/** Fired once when a game starts or ends (e.g. king capture); ENDED carries the winner. */
public record GameLifecycleEvent(Phase phase, PieceColor winner) implements GameEvent {
    public enum Phase { STARTED, ENDED }
}