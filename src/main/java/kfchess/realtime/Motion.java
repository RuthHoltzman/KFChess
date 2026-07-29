package kfchess.realtime;

import kfchess.model.Piece;
import kfchess.model.Position;


/** A piece sliding from one square to another over time, used to interpolate its on-screen position. */
public record Motion(Piece piece, Position from, Position to, long startTime, long arrivalTime) {

    /** Whether the motion has reached its destination as of the given clock time. */
    public boolean hasArrived(long clock) {
        return clock >= arrivalTime;
    }

    /** How far through the motion "now" is, from 0.0 (start) to 1.0 (arrived) - used to interpolate pixel position. */
    public double progress(long now) {
        long duration = arrivalTime - startTime;
        if (duration <= 0) {
            return 1.0;
        }
        double p = (now - startTime) / (double) duration;
        if (p < 0) return 0.0;
        if (p > 1) return 1.0;
        return p;
    }
}
