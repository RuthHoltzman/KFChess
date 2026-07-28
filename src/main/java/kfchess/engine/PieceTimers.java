package kfchess.engine;

import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Tracks per-piece jump/rest cooldowns - what makes the "sandglass" timers a real game rule, not just visual. */
public class PieceTimers {

    private static final long DEFAULT_JUMP_DURATION_MS = 1000;
    public static final long SHORT_REST_DURATION_MS = 500;  // after a normal move
    public static final long LONG_REST_DURATION_MS = 1000;  // after a jump
    private final Map<Piece, Long> jumpEndTimes = new HashMap<>();
    private final Map<Piece, Long> jumpStartTimes = new HashMap<>();
    private final Map<Piece, Long> restEndTimes = new HashMap<>();

    /** Marks a piece as jumping and schedules when the jump ends. */
    public void beginJump(Piece piece, long now) {
        piece.markJumping();
        jumpStartTimes.put(piece, now);
        jumpEndTimes.put(piece, now + DEFAULT_JUMP_DURATION_MS);
    }

    /** Starts the (shorter) cooldown after a normal move. */
    public void beginShortRest(Piece piece, long now) {
        restEndTimes.put(piece, now + SHORT_REST_DURATION_MS);
    }

    /** A piece can act if it's idle and its rest cooldown (if any) has already passed. */
    public boolean isAvailableToAct(Piece piece, long now) {
        if (!piece.isIdle()) {
            return false;
        }
        Long restEndTime = restEndTimes.get(piece);
        return restEndTime == null || now >= restEndTime;
    }

    /** Ends any jumps whose duration is over, and starts the (longer) post-jump rest. */
    public void resolveExpiredJumps(long now) {
        List<Piece> finishedJumpers = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : jumpEndTimes.entrySet()) {
            if (now >= entry.getValue()) {
                finishedJumpers.add(entry.getKey());
            }
        }
        for (Piece piece : finishedJumpers) {
            piece.markJumpEnded();
            jumpEndTimes.remove(piece);
            jumpStartTimes.remove(piece);
            restEndTimes.put(piece, now + LONG_REST_DURATION_MS);
        }
    }

    /** Drops rest-cooldown entries that have already elapsed. */
    public void purgeExpiredRest(long now) {
        restEndTimes.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    /** Snapshot of every piece currently mid-jump, for drawing/broadcasting. */
    public List<JumpVisual> activeJumps() {
        List<JumpVisual> jumps = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : jumpEndTimes.entrySet()) {
            Piece piece = entry.getKey();
            long endTime = entry.getValue();
            long startTime = jumpStartTimes.getOrDefault(piece, endTime - DEFAULT_JUMP_DURATION_MS);
            jumps.add(new JumpVisual(piece, startTime, endTime));
        }
        return jumps;
    }
}
