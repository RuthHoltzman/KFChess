package kfchess.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers decoding incoming client messages ({"type":"CLICK","row":..,"col":..}) and the basic
 * validity check applied before a command is routed to PlayCommandController.
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
        // RESTART has no board position at all, so row/col are not required.
        ClientCommand command = gson.fromJson("{\"type\":\"RESTART\"}", ClientCommand.class);

        assertEquals(ClientCommandType.RESTART, command.type());
        assertTrue(command.isValid());
    }

    @Test
    void fromJson_restartCommandWithDummyRowAndCol_isValid() {
        // This is how PlayClient.sendRestart() actually sends it (dummy 0,0) - also valid.
        ClientCommand command = gson.fromJson("{\"type\":\"RESTART\",\"row\":0,\"col\":0}", ClientCommand.class);

        assertTrue(command.isValid());
    }
}
