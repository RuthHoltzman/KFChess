package texttests;

import kfchess.HomeScreenMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeScreenMainTest {

    @Test
    void buildUri_normalRoom_appendsToServerAddress() {
        assertEquals("ws://localhost:8887/room1", HomeScreenMain.buildUri("room1"));
    }

    @Test
    void buildUri_emptyRoom_fallsBackToDefault() {
        assertEquals("ws://localhost:8887/default", HomeScreenMain.buildUri(""));
    }

    @Test
    void buildUri_blankRoom_fallsBackToDefault() {
        assertEquals("ws://localhost:8887/default", HomeScreenMain.buildUri("   "));
    }

    @Test
    void buildUri_nullRoom_fallsBackToDefault() {
        assertEquals("ws://localhost:8887/default", HomeScreenMain.buildUri(null));
    }

    @Test
    void buildUri_roomWithSurroundingWhitespace_isTrimmed() {
        assertEquals("ws://localhost:8887/room1", HomeScreenMain.buildUri("  room1  "));
    }
}
