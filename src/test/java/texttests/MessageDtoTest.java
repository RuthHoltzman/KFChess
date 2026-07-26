package texttests;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kfchess.server.ErrorMessage;
import kfchess.server.RoleAssignedMessage;
import kfchess.server.SnapshotMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את צורת ה-JSON היוצא של הודעות השרת->לקוח: בעיקר ששדה "type"
 * יוצא נכון (זה מה שהלקוח משתמש בו כדי להבחין בין סוגי הודעות), ושערכי
 * null (winner/selected לפני שיש בחירה/ניצחון) לא גורמים לחריגה.
 */
class MessageDtoTest {

    private final Gson gson = new Gson();

    @Test
    void roleAssignedMessage_serializesTypeRoleAndGameId() {
        JsonObject json = gson.toJsonTree(new RoleAssignedMessage("WHITE", "default")).getAsJsonObject();

        assertEquals("ROLE_ASSIGNED", json.get("type").getAsString());
        assertEquals("WHITE", json.get("role").getAsString());
        assertEquals("default", json.get("gameId").getAsString());
    }

    @Test
    void errorMessage_serializesTypeAndMessage() {
        JsonObject json = gson.toJsonTree(new ErrorMessage("bad command")).getAsJsonObject();

        assertEquals("ERROR", json.get("type").getAsString());
        assertEquals("bad command", json.get("message").getAsString());
    }

    @Test
    void snapshotMessage_withNullSelectedAndWinner_serializesWithoutThrowing() {
        SnapshotMessage snapshot = new SnapshotMessage(
                8, 8, List.of(), null, List.of(), Map.of(), Map.of(), false, null, 1234L,
                List.of(), List.of(), List.of());

        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();

        assertEquals("SNAPSHOT", json.get("type").getAsString());
        assertFalse(json.has("selected"));
        assertFalse(json.has("winner"));
        assertFalse(json.get("gameOver").getAsBoolean());
        assertEquals(1234, json.get("now").getAsLong());
    }

    @Test
    void snapshotMessage_gameOverWithWinner_serializesWinnerField() {
        SnapshotMessage snapshot = new SnapshotMessage(
                8, 8, List.of(), null, List.of(), Map.of(), Map.of(), true, "WHITE", 5000L,
                List.of(), List.of(), List.of());

        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();

        assertTrue(json.get("gameOver").getAsBoolean());
        assertEquals("WHITE", json.get("winner").getAsString());
    }

    // שלב 5, חלק 1: שדה חדש (ר' SnapshotMessage.disconnectSecondsRemaining) - Integer
    // ולא int/boolean בכוונה, כדי שאפשר יהיה להבדיל "0 שניות נשארו" מ-"אין ניתוק פעיל".

    @Test
    void snapshotMessage_withDisconnectSecondsRemaining_serializesField() {
        SnapshotMessage snapshot = new SnapshotMessage(
                8, 8, List.of(), null, List.of(), Map.of(), Map.of(), false, null, 1234L,
                List.of(), List.of(), List.of(), false, 15);

        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();

        assertEquals(15, json.get("disconnectSecondsRemaining").getAsInt());
    }

    @Test
    void snapshotMessage_withoutDisconnectSecondsRemaining_omitsFieldFromJson() {
        SnapshotMessage snapshot = new SnapshotMessage(
                8, 8, List.of(), null, List.of(), Map.of(), Map.of(), false, null, 1234L,
                List.of(), List.of(), List.of());

        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();

        assertFalse(json.has("disconnectSecondsRemaining")); // null -> Gson משמיט את השדה לגמרי, לא כותב "null"
    }

    // בקשת רות (הסבב הזה): שדה חדש waitingForOpponent - boolean רגיל,
    // תמיד משודר (בניגוד ל-disconnectSecondsRemaining שהוא Integer ויכול
    // להיות null/מושמט).

    @Test
    void snapshotMessage_withWaitingForOpponentTrue_serializesField() {
        SnapshotMessage snapshot = new SnapshotMessage(
                8, 8, List.of(), null, List.of(), Map.of(), Map.of(), false, null, 1234L,
                List.of(), List.of(), List.of(), false, null, true);

        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();

        assertTrue(json.get("waitingForOpponent").getAsBoolean());
    }

    @Test
    void snapshotMessage_oldConstructorWithoutWaitingForOpponent_defaultsToFalse() {
        SnapshotMessage snapshot = new SnapshotMessage(
                8, 8, List.of(), null, List.of(), Map.of(), Map.of(), false, null, 1234L,
                List.of(), List.of(), List.of());

        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();

        assertFalse(json.get("waitingForOpponent").getAsBoolean());
    }
}
