package kfchess.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeScreenTest {

    @Test
    void buildUri_normalRoom_appendsToServerAddress() {
        assertEquals("ws://localhost:8887/room1", HomeScreen.buildUri("room1", null));
    }

    @Test
    void buildUri_emptyRoom_fallsBackToDefault() {
        assertEquals("ws://localhost:8887/default", HomeScreen.buildUri("", null));
    }

    @Test
    void buildUri_blankRoom_fallsBackToDefault() {
        assertEquals("ws://localhost:8887/default", HomeScreen.buildUri("   ", null));
    }

    @Test
    void buildUri_nullRoom_fallsBackToDefault() {
        assertEquals("ws://localhost:8887/default", HomeScreen.buildUri(null, null));
    }

    @Test
    void buildUri_roomWithSurroundingWhitespace_isTrimmed() {
        assertEquals("ws://localhost:8887/room1", HomeScreen.buildUri("  room1  ", null));
    }

    @Test
    void buildUri_withUsername_appendsAsQueryParameter() {
        assertEquals("ws://localhost:8887/room1?username=ruth", HomeScreen.buildUri("room1", "ruth"));
    }

    @Test
    void buildUri_withNullUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/room1", HomeScreen.buildUri("room1", null));
    }

    @Test
    void buildUri_withBlankUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/room1", HomeScreen.buildUri("room1", "   "));
    }

    @Test
    void buildUri_withUsernameContainingSpecialCharacters_urlEncodesIt() {
        assertEquals("ws://localhost:8887/room1?username=ruth+h", HomeScreen.buildUri("room1", "ruth h"));
    }

    // buildMatchmakingUri ignores the room name entirely and always targets the
    // reserved path that MatchmakingResolver recognizes on the server side.

    @Test
    void buildMatchmakingUri_withoutUsername_pointsToMatchmakingPath() {
        assertEquals("ws://localhost:8887/_play", HomeScreen.buildMatchmakingUri(null));
    }

    @Test
    void buildMatchmakingUri_withUsername_appendsAsQueryParameter() {
        assertEquals("ws://localhost:8887/_play?username=ruth", HomeScreen.buildMatchmakingUri("ruth"));
    }

    @Test
    void buildMatchmakingUri_withBlankUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/_play", HomeScreen.buildMatchmakingUri("   "));
    }

    // buildCreateRoomUri uses its own reserved path, separate from matchmaking,
    // and likewise ignores any room name.

    @Test
    void buildCreateRoomUri_withoutUsername_pointsToCreateRoomPath() {
        assertEquals("ws://localhost:8887/_create", HomeScreen.buildCreateRoomUri(null));
    }

    @Test
    void buildCreateRoomUri_withUsername_appendsAsQueryParameter() {
        assertEquals("ws://localhost:8887/_create?username=ruth", HomeScreen.buildCreateRoomUri("ruth"));
    }

    @Test
    void buildCreateRoomUri_withBlankUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/_create", HomeScreen.buildCreateRoomUri("   "));
    }
}
