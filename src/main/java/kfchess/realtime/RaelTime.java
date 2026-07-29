package kfchess.realtime;


/** The game's own clock (not wall-clock time) - lets tests "jump" time forward without a real Thread.sleep. */
public class RaelTime {

    private long currentTime = 0;

    /** The current game-clock time, in milliseconds. */
    public long now() {
        return currentTime;
    }

    /** Moves the game clock forward by the given elapsed milliseconds. */
    public void advance(long milliseconds) {
        if (milliseconds > 0) {
            currentTime += milliseconds;
        }
    }
}
