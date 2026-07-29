package kfchess.protocol;


/** Outgoing-only DTO sent once per connection right after onOpen, telling the client its role and game id. */
public class RoleAssignedMessage {

    private final String type = "ROLE_ASSIGNED";
    private final String role;
    private final String gameId;

    public RoleAssignedMessage(String role, String gameId) {
        this.role = role;
        this.gameId = gameId;
    }
}
