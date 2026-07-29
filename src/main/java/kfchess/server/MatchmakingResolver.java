package kfchess.server;

/** Detects a matchmaking connection request by its reserved path, rather than a named room. */
public final class MatchmakingResolver {

    // Must match the token in HomeScreen.buildMatchmakingUri exactly - both ends define it separately.
    // Known limitation: typing "_play" as a manual room name collides with matchmaking. Rare enough to ignore.
    private static final String MATCHMAKING_PATH = "_play";

    private MatchmakingResolver() {
    }

    /** Whether this connection path is the reserved matchmaking path, with or without a query string. */
    public static boolean isMatchmakingRequest(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return false;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.equals(MATCHMAKING_PATH);
    }
}
