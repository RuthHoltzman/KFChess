package kfchess.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomIdGeneratorTest {

    @Test
    void generate_returnsSixCharacterCode() {
        assertEquals(6, RoomIdGenerator.generate().length());
    }

    @Test
    void generate_onlyUppercaseLettersAndDigits() {
        assertTrue(RoomIdGenerator.generate().matches("[A-Z0-9]{6}"));
    }

    @Test
    void generate_twoCallsProduceDifferentCodes() {
        // Not a mathematical guarantee (a collision is possible in theory), but with about
        // 2 billion combinations two consecutive calls returning the same code is effectively
        // impossible - enough as a sanity check that the class isn't returning a constant.
        assertNotEquals(RoomIdGenerator.generate(), RoomIdGenerator.generate());
    }
}
