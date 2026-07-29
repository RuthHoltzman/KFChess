package kfchess.protocol;

/**
 * The reserved connection-path tokens the client and the server must agree on.
 * <p>
 * These used to be declared separately at each end, so changing one side would have broken the
 * other silently at runtime, with nothing for the compiler to catch. They live in the protocol
 * package because that is exactly what protocol means here: the shared contract between the two.
 */
public final class ConnectionPaths {

    /** Reserved path for a matchmaking ("Play") connection. */
    public static final String MATCHMAKING = "_play";

    /** Reserved path for a "Create room" connection. */
    public static final String CREATE_ROOM = "_create";

    /** The room used when the connection path names none. */
    public static final String DEFAULT_ROOM = "default";

    private ConnectionPaths() {
    }
}
