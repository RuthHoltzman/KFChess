package kfchess.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את פענוח פרוטוקול ההודעות הנכנסות מהלקוח (ר' ההחלטה ב-PROGRESS.md:
 * {"type":"CLICK","row":..,"col":..}) ואת בדיקת התקינות הבסיסית לפני
 * שהפקודה מנותבת ל-NetworkActions.
 */
class ClientCommandTest {

    private final Gson gson = new Gson();

    @Test
    void fromJson_validClickCommand_parsesTypeAndPosition() {
        ClientCommand command = gson.fromJson("{\"type\":\"CLICK\",\"row\":6,\"col\":4}", ClientCommand.class);

        assertEquals(ClientCommandType.CLICK, command.type());
        assertEquals(6, command.row());
        assertEquals(4, command.col());
        assertTrue(command.isValid());
    }

    @Test
    void fromJson_validJumpCommand_parsesType() {
        ClientCommand command = gson.fromJson("{\"type\":\"JUMP\",\"row\":0,\"col\":0}", ClientCommand.class);

        assertEquals(ClientCommandType.JUMP, command.type());
        assertTrue(command.isValid());
    }

    @Test
    void isValid_missingRow_returnsFalse() {
        ClientCommand command = gson.fromJson("{\"type\":\"CLICK\",\"col\":4}", ClientCommand.class);

        assertFalse(command.isValid());
    }

    @Test
    void isValid_missingType_returnsFalse() {
        ClientCommand command = gson.fromJson("{\"row\":6,\"col\":4}", ClientCommand.class);

        assertFalse(command.isValid());
    }

    @Test
    void fromJson_restartCommandWithoutRowOrCol_isValid() {
        // RESTART אין לו מיקום על הלוח בכלל - פטור מ-row/col (ר' ClientCommand.isValid()).
        ClientCommand command = gson.fromJson("{\"type\":\"RESTART\"}", ClientCommand.class);

        assertEquals(ClientCommandType.RESTART, command.type());
        assertTrue(command.isValid());
    }

    @Test
    void fromJson_restartCommandWithDummyRowAndCol_isValid() {
        // ככה GameClient.sendRestart() בפועל שולח אותו (0,0 דמה) - גם זה תקין.
        ClientCommand command = gson.fromJson("{\"type\":\"RESTART\",\"row\":0,\"col\":0}", ClientCommand.class);

        assertTrue(command.isValid());
    }
}
