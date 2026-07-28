package kfchess.bus;

/** Marker for everything that can travel through the {@link EventBus} - the 4 event types below. */
public sealed interface GameEvent
        permits ScoreUpdatedEvent, MoveLoggedEvent, SoundEvent, GameLifecycleEvent {
}