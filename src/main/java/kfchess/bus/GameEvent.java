package kfchess.bus;

public sealed interface GameEvent
        permits ScoreUpdatedEvent, MoveLoggedEvent, SoundEvent, GameLifecycleEvent {
}