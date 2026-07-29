package kfchess.engine.snapshot;

import kfchess.model.Piece;
import kfchess.model.Position;

import java.util.ArrayList;
import java.util.List;


/** Tracks recently-captured pieces so the view can draw a short fade-out effect for them. */
public class CaptureEffectTracker {

    public static final long CAPTURE_EFFECT_DURATION_MS = 450;

    private final List<CaptureEffect> recentCaptures = new ArrayList<>();

    /** Starts a fade-out effect for a piece that was just captured. */
    public void register(Piece removedPiece, Position at, long now) {
        recentCaptures.add(new CaptureEffect(removedPiece.kind(), removedPiece.color(), at, now));
    }

    /** Drops effects whose fade duration has already elapsed. */
    public void purgeExpired(long now) {
        recentCaptures.removeIf(effect -> now - effect.removedAt() >= CAPTURE_EFFECT_DURATION_MS);
    }

    /** Snapshot of every capture effect still worth drawing. */
    public List<CaptureEffect> active() {
        return List.copyOf(recentCaptures);
    }
}
