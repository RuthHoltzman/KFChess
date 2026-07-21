package texttests;

import kfchess.net.IncomingMessageSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את תרגום הודעות השרת לשורה קריאה אחת - לוגיקה טהורה, בלי חיבור
 * רשת (ר' GameIdResolverTest לאותו רעיון בצד ה-gameId).
 */
class IncomingMessageSummaryTest {

    @Test
    void describe_roleAssigned_includesRoleAndGameId() {
        String json = "{\"type\":\"ROLE_ASSIGNED\",\"role\":\"WHITE\",\"gameId\":\"default\"}";

        assertEquals("[ROLE_ASSIGNED] role=WHITE gameId=default", IncomingMessageSummary.describe(json));
    }

    @Test
    void describe_snapshot_includesNowPiecesAndGameOver() {
        String json = "{\"type\":\"SNAPSHOT\",\"now\":1234,\"pieces\":[{},{},{}],\"gameOver\":false}";

        assertEquals("[SNAPSHOT] now=1234 pieces=3 gameOver=false", IncomingMessageSummary.describe(json));
    }

    @Test
    void describe_error_includesMessage() {
        String json = "{\"type\":\"ERROR\",\"message\":\"bad command\"}";

        assertEquals("[ERROR] bad command", IncomingMessageSummary.describe(json));
    }

    @Test
    void describe_unknownType_fallsBackToRawJson() {
        String json = "{\"type\":\"SOMETHING_ELSE\"}";

        assertTrue(IncomingMessageSummary.describe(json).startsWith("[UNKNOWN]"));
    }
}
