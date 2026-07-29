package kfchess.server;

import kfchess.protocol.ConnectionPaths;

/** Detects a "Create room" connection request by its reserved path. Pure and separately unit-testable. */
public final class CreateRoomResolver {

    private CreateRoomResolver() {
    }

    /** Whether this connection path is the reserved "create a new room" path. */
    public static boolean isCreateRoomRequest(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return false;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.equals(ConnectionPaths.CREATE_ROOM);
    }
}
