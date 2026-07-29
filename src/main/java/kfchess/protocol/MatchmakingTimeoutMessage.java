package kfchess.protocol;


/** Outgoing-only DTO sent when a waiting player found no ELO-matched opponent within the timeout. */
public class MatchmakingTimeoutMessage {

    private final String type = "MATCHMAKING_TIMEOUT";
    private final String message;

    public MatchmakingTimeoutMessage(String message) {
        this.message = message;
    }
}
