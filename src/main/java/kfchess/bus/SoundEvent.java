package kfchess.bus;


/** Fired to trigger a sound effect for a move/capture/illegal-action. */
public record SoundEvent(Type type) implements PlayEvent {
    public enum Type { MOVE, CAPTURE, ILLEGAL }
}