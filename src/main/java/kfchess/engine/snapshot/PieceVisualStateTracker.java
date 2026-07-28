package kfchess.engine.snapshot;

import kfchess.engine.PieceTimers;
import kfchess.model.Piece;
import kfchess.model.PieceState;
import java.util.HashMap;
import java.util.Map;

/** Derives each piece's visual state (idle/moving/jumping/resting) from its logical state over time. */
public class PieceVisualStateTracker {


    private static final long SHORT_REST_MS = PieceTimers.SHORT_REST_DURATION_MS;
    private static final long LONG_REST_MS = PieceTimers.LONG_REST_DURATION_MS;

    /** Per-piece bookkeeping: last known logical state, current visual state, and when it started. */
    private static class Entry {
        PieceState lastKnownLogicalState;
        PieceVisualState visualState;
        long visualStateEnteredAt;
    }

    private final Map<Piece, Entry> entries = new HashMap<>();

/** Updates and returns the piece's current visual state, transitioning it as its logical state changes. */
public PieceVisualState resolve(Piece piece, long now) {
    Entry entry = entries.computeIfAbsent(piece, p -> {
        Entry e = new Entry();
        e.lastKnownLogicalState = p.state();
        e.visualState = PieceVisualState.IDLE;
        e.visualStateEnteredAt = now;
        return e;
    });

    PieceState currentLogicalState = piece.state();

    if (currentLogicalState != entry.lastKnownLogicalState) {
        if (entry.lastKnownLogicalState == PieceState.IN_TRANSIT
                && currentLogicalState == PieceState.IDLE) {
            entry.visualState = PieceVisualState.SHORT_REST;
            entry.visualStateEnteredAt = now;
        } else if (entry.lastKnownLogicalState == PieceState.JUMPING
                && currentLogicalState == PieceState.IDLE) {
            entry.visualState = PieceVisualState.LONG_REST;
            entry.visualStateEnteredAt = now;
        }
        entry.lastKnownLogicalState = currentLogicalState;
    }

    if (currentLogicalState == PieceState.IN_TRANSIT) {
        entry.visualState = PieceVisualState.MOVING;
    } else if (currentLogicalState == PieceState.JUMPING) {
        entry.visualState = PieceVisualState.JUMPING;
    }

    if (entry.visualState == PieceVisualState.SHORT_REST
            && now - entry.visualStateEnteredAt >= SHORT_REST_MS) {
        entry.visualState = PieceVisualState.IDLE;
        entry.visualStateEnteredAt = now;
    } else if (entry.visualState == PieceVisualState.LONG_REST
            && now - entry.visualStateEnteredAt >= LONG_REST_MS) {
        entry.visualState = PieceVisualState.IDLE;
        entry.visualStateEnteredAt = now;
    }

    return entry.visualState;
}

    /** How long the piece has been in its current visual state, in milliseconds. */
    public long elapsedInCurrentVisualState(Piece piece, long now) {
        Entry entry = entries.get(piece);
        return entry == null ? 0 : now - entry.visualStateEnteredAt;
    }


    /** How far through its rest cooldown the piece is, from 0.0 (just started) to 1.0 (done). */
    public double restProgress(Piece piece, long now) {
        Entry entry = entries.get(piece);
        if (entry == null) {
            return 0;
        }
        long totalMs = switch (entry.visualState) {
            case SHORT_REST -> SHORT_REST_MS;
            case LONG_REST -> LONG_REST_MS;
            default -> 0;
        };
        if (totalMs <= 0) {
            return 0;
        }
        long elapsed = now - entry.visualStateEnteredAt;
        return Math.max(0.0, Math.min(1.0, elapsed / (double) totalMs));
    }
}