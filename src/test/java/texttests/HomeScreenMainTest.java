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

    @Test
    void buildUri_withUsername_appendsAsQueryParameter() {
        assertEquals("ws://localhost:8887/room1?username=ruth", HomeScreenMain.buildUri("room1", "ruth"));
    }

    @Test
    void buildUri_withNullUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/room1", HomeScreenMain.buildUri("room1", null));
    }

    @Test
    void buildUri_withBlankUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/room1", HomeScreenMain.buildUri("room1", "   "));
    }

    @Test
    void buildUri_withUsernameContainingSpecialCharacters_urlEncodesIt() {
        assertEquals("ws://localhost:8887/room1?username=ruth+h", HomeScreenMain.buildUri("room1", "ruth h"));
    }

    // שלב 5, חלק 2: buildMatchmakingUri (כפתור "Skip") - מתעלמת לגמרי משם ה-room,
    // מתחברת תמיד לנתיב השמור שגם MatchmakingResolver בצד השרת מזהה.

    @Test
    void buildMatchmakingUri_withoutUsername_pointsToMatchmakingPath() {
        assertEquals("ws://localhost:8887/_play", HomeScreenMain.buildMatchmakingUri(null));
    }

    @Test
    void buildMatchmakingUri_withUsername_appendsAsQueryParameter() {
        assertEquals("ws://localhost:8887/_play?username=ruth", HomeScreenMain.buildMatchmakingUri("ruth"));
    }

    @Test
    void buildMatchmakingUri_withBlankUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/_play", HomeScreenMain.buildMatchmakingUri("   "));
    }

    // שלב 6: buildCreateRoomUri (כפתור "Create" בדיאלוג Room) - נתיב שמור
    // נפרד מ-matchmaking, מתעלם לגמרי מכל שם room.

    @Test
    void buildCreateRoomUri_withoutUsername_pointsToCreateRoomPath() {
        assertEquals("ws://localhost:8887/_create", HomeScreenMain.buildCreateRoomUri(null));
    }

    @Test
    void buildCreateRoomUri_withUsername_appendsAsQueryParameter() {
        assertEquals("ws://localhost:8887/_create?username=ruth", HomeScreenMain.buildCreateRoomUri("ruth"));
    }

    @Test
    void buildCreateRoomUri_withBlankUsername_sameAsWithoutUsername() {
        assertEquals("ws://localhost:8887/_create", HomeScreenMain.buildCreateRoomUri("   "));
    }
}
