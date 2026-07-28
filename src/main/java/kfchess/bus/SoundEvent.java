package kfchess.bus;


/** Fired to trigger a sound effect for a move/capture/illegal-action. */
public record SoundEvent(Type type) implements GameEvent {
    public enum Type { MOVE, CAPTURE, ILLEGAL }
}