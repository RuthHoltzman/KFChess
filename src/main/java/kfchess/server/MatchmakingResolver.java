package kfchess.server;

import kfchess.protocol.ConnectionPaths;

/**
 * Detects a matchmaking connection request by its reserved path, rather than a named room.
 * <p>
 * Known limitation: typing the reserved token as a manual room name collides with matchmaking.
 * Rare enough in practice to leave alone.
 */
public final class MatchmakingResolver {

    private MatchmakingResolver() {
    }

    /** Whether this connection path is the reserved matchmaking path, with or without a query string. */
    public static boolean isMatchmakingRequest(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return false;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.equals(ConnectionPaths.MATCHMAKING);
    }
}
