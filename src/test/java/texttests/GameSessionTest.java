package texttests;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kfchess.net.ClientCommand;
import kfchess.net.ClientRole;
import kfchess.net.server.GameSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את GameSession: הקצאת תפקידים, ניתוב פקודות מהתור אל NetworkActions
 * ב-tick, והתוכן של ה-snapshot שנבנה לכל צבע. ה-WebSocket משמש כאן רק
 * כמפתח-זהות (ר' FakeWebSocket) - אין חיבור רשת אמיתי בטסטים האלה.
 */
class GameSessionTest {

    private final Gson gson = new Gson();

    private ClientCommand click(int row, int col) {
        return gson.fromJson("{\"type\":\"CLICK\",\"row\":" + row + ",\"col\":" + col + "}", ClientCommand.class);
    }

    @Test
    void assignRole_firstConnectionIsWhite_secondIsBlack_restAreSpectators() {
        GameSession session = new GameSession();

        assertEquals(ClientRole.WHITE, session.assignRole(new FakeWebSocket()));
        assertEquals(ClientRole.BLACK, session.assignRole(new FakeWebSocket()));
        assertEquals(ClientRole.SPECTATOR, session.assignRole(new FakeWebSocket()));
    }

    @Test
    void removeConnection_removesItFromConnectionsMap() {
        GameSession session = new GameSession();
        FakeWebSocket connection = new FakeWebSocket();
        session.assignRole(connection);

        session.removeConnection(connection);

        assertFalse(session.connections().containsKey(connection));
    }

    @Test
    void tick_clickOnOwnPiece_selectsItForThatColorOnly() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white);
        session.assignRole(black);

        session.enqueueCommand(white, click(6, 4)); // חייל לבן בשורת הפתיחה
        session.tick(0);

        JsonObject whiteView = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonObject selected = whiteView.getAsJsonObject("selected");
        assertEquals(6, selected.get("row").getAsInt());
        assertEquals(4, selected.get("col").getAsInt());

        JsonObject blackView = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertFalse(blackView.has("selected")); // הבחירה של הלבן לא "דולפת" לצבע אחר
    }

    @Test
    void tick_clickThenLegalTarget_movesPieceIntoTransit() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);

        session.enqueueCommand(white, click(6, 4)); // בחירה
        session.enqueueCommand(white, click(5, 4)); // צעד אחד קדימה - חוקי
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonArray pieces = snapshot.getAsJsonArray("pieces");

        boolean foundInTransit = false;
        for (JsonElement element : pieces) {
            JsonObject pieceDto = element.getAsJsonObject();
            JsonObject position = pieceDto.getAsJsonObject("position");
            if (position.get("row").getAsInt() == 6 && position.get("col").getAsInt() == 4) {
                assertEquals("IN_TRANSIT", pieceDto.getAsJsonObject("piece").get("state").getAsString());
                foundInTransit = true;
            }
        }
        assertTrue(foundInTransit);
    }

    @Test
    void tick_clickThenLegalTarget_snapshotIncludesMatchingMotion() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);

        session.enqueueCommand(white, click(6, 4));
        session.enqueueCommand(white, click(5, 4));
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonArray motions = snapshot.getAsJsonArray("motions");

        assertEquals(1, motions.size());
        JsonObject motion = motions.get(0).getAsJsonObject();
        assertEquals(6, motion.getAsJsonObject("from").get("row").getAsInt());
        assertEquals(5, motion.getAsJsonObject("to").get("row").getAsInt());
    }

    @Test
    void tick_spectatorCommand_isIgnored() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE
        session.assignRole(new FakeWebSocket()); // BLACK
        FakeWebSocket spectator = new FakeWebSocket();
        assertEquals(ClientRole.SPECTATOR, session.assignRole(spectator));

        session.enqueueCommand(spectator, click(6, 4));
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.SPECTATOR)).getAsJsonObject();
        assertFalse(snapshot.has("selected"));
    }

    @Test
    void tick_commandFromUnknownConnection_doesNotThrow() {
        GameSession session = new GameSession();
        FakeWebSocket unregistered = new FakeWebSocket(); // מעולם לא עבר assignRole

        session.enqueueCommand(unregistered, click(6, 4));

        assertDoesNotThrow(() -> session.tick(0));
    }

    @Test
    void snapshotFor_initialBoard_hasThirtyTwoPiecesAndZeroScores() {
        GameSession session = new GameSession();

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.SPECTATOR)).getAsJsonObject();

        assertEquals(32, snapshot.getAsJsonArray("pieces").size());
        assertFalse(snapshot.get("gameOver").getAsBoolean());
        assertFalse(snapshot.has("winner"));
        JsonObject scores = snapshot.getAsJsonObject("scores");
        assertEquals(0, scores.get("WHITE").getAsInt());
        assertEquals(0, scores.get("BLACK").getAsInt());
        assertEquals(0, snapshot.getAsJsonArray("motions").size());
        assertEquals(0, snapshot.getAsJsonArray("jumps").size());
        assertEquals(0, snapshot.getAsJsonArray("captureEffects").size());
    }
}
