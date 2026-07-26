package texttests;

import com.google.gson.Gson;
import kfchess.server.ClientRole;
import kfchess.server.RoleAssignedMessage;
import kfchess.server.client.GameClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * onMessage() עצמה לא נוגעת ברשת בכלל (רק מפענחת JSON ושומרת בשדות) - אז
 * אפשר לבנות GameClient עם URI דמה (בלי connectBlocking(), בדיוק כמו
 * RecordingGameClient הקיים) ולקרוא לה ישירות, בלי שרת אמיתי. מתמקד
 * ב-assignedRole() (חדש, בקשת רות - "צבע שחקן או צופה" על המסך) - עד
 * עכשיו הודעת ROLE_ASSIGNED נפרסה רק לגבי gameId, ה-role שלה הוזנח.
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
