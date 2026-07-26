package kfchess.bus;

/** מתפרסם כדי לבקש מה-UI לנגן אפקט קול מתאים. */
public record SoundEvent(Type type) implements GameEvent {
    public enum Type { MOVE, CAPTURE, ILLEGAL }
}