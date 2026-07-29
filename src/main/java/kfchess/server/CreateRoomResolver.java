package kfchess.server;

/** Detects a "Create room" connection request by its reserved path. Pure and separately unit-testable. */
public final class CreateRoomResolver {

    // Must match the token in HomeScreen.buildCreateRoomUri exactly - both ends define it separately.
    private static final String CREATE_ROOM_PATH = "_create";

    private CreateRoomResolver() {
    }

    /** Whether this connection path is the reserved "create a new room" path. */
    public static boolean isCreateRoomRequest(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return false;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.equals(CREATE_ROOM_PATH);
    }
}
