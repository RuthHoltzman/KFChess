package texttests;

import kfchess.net.IncomingMessageSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את תרגום הודעות השרת לשורה קריאה אחת, ואת isSnapshot() (שמשמש
 * את GameClient כדי להחליט אם להדפיס מיד או לשמור בשקט) - לוגיקה
 * טהורה, בלי חיבור רשת (ר' GameIdResolverTest לאותו רעיון).
 */
class IncomingMessageSummaryTest {

    @Test
    void describe_roleAssigned_includesRoleAndGameId() {
        String json = "{\"type\":\"ROLE_ASSIGNED\",\"role\":\"WHITE\",\"gameId\":\"default\"}";

        assertEquals("[ROLE_ASSIGNED] role=WHITE gameId=default", IncomingMessageSummary.describe(json));
    }

    @Test
    void describe_snapshotWithoutSelection_showsSelectedNone() {
        String json = "{\"type\":\"SNAPSHOT\",\"now\":1234,\"pieces\":[{},{},{}],\"gameOver\":false}";

        assertEquals("[SNAPSHOT] now=1234 pieces=3 gameOver=false selected=none",
                IncomingMessageSummary.describe(json));
    }

    @Test
    void describe_snapshotWithSelection_showsSelectedRowCol() {
        String json = "{\"type\":\"SNAPSHOT\",\"now\":1234,\"pieces\":[],\"gameOver\":false,"
                + "\"selected\":{\"row\":6,\"col\":4}}";

        assertEquals("[SNAPSHOT] now=1234 pieces=0 gameOver=false selected=(6,4)",
                IncomingMessageSummary.describe(json));
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

    @Test
    void isSnapshot_snapshotMessage_returnsTrue() {
        assertTrue(IncomingMessageSummary.isSnapshot("{\"type\":\"SNAPSHOT\"}"));
    }

    @Test
    void isSnapshot_otherMessageTypes_returnFalse() {
        assertFalse(IncomingMessageSummary.isSnapshot("{\"type\":\"ROLE_ASSIGNED\"}"));
        assertFalse(IncomingMessageSummary.isSnapshot("{\"type\":\"ERROR\"}"));
    }
}
