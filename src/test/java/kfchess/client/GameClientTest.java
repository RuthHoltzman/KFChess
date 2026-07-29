package kfchess.client;

import com.google.gson.Gson;
import kfchess.model.ClientRole;
import kfchess.protocol.RoleAssignedMessage;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * onMessage() never touches the network - it only decodes JSON into fields - so the client can be
 * built with a dummy URI (no connectBlocking()) and called directly, with no real server.
 * These tests focus on assignedRole(), which ROLE_ASSIGNED parsing originally ignored.
 */
class GameClientTest {

    private final Gson gson = new Gson();

    private static GameClient newClient() throws URISyntaxException {
        return new GameClient(new URI("ws://localhost:1"));
    }

    @Test
    void onMessage_roleAssignedWhite_setsAssignedRoleAndGameId() throws URISyntaxException {
        GameClient client = newClient();
        client.onMessage(gson.toJson(new RoleAssignedMessage("WHITE", "room1")));

        assertEquals(ClientRole.WHITE, client.assignedRole());
        assertEquals("room1", client.assignedGameId());
    }

    @Test
    void onMessage_roleAssignedSpectator_setsAssignedRoleToSpectator() throws URISyntaxException {
        GameClient client = newClient();
        client.onMessage(gson.toJson(new RoleAssignedMessage("SPECTATOR", "room2")));

        assertEquals(ClientRole.SPECTATOR, client.assignedRole());
    }

    @Test
    void assignedRole_beforeAnyMessage_isNull() throws URISyntaxException {
        GameClient client = newClient();

        assertNull(client.assignedRole());
    }
}
