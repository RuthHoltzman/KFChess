package kfchess.server;

import kfchess.protocol.ConnectionPaths;

/** Turns the WebSocket connection path into a gameId. A separate class so it's unit-testable without a real server. */
public final class PlayIdResolver {

    private PlayIdResolver() {
    }

    /** "/room1?username=ruth" -&gt; "room1"; the root path (or null) falls back to the default room. */
    public static String resolve(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return ConnectionPaths.DEFAULT_ROOM;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.isEmpty() ? ConnectionPaths.DEFAULT_ROOM : trimmed;
    }
}
