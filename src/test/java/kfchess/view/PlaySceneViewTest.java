package kfchess.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PlaySceneView itself isn't tested directly (drawing and Swing), but shortRoomId() is completely
 * pure, so it's worth testing like any other meaningful logic.
 * Matchmaking ids ("match-&lt;uuid&gt;") are shortened to the first 8 UUID characters; Create/Join
 * codes are left untouched.
 */
class PlaySceneViewTest {

    @Test
    void shortRoomId_matchmakingId_truncatesToFirst8UuidChars() {
        assertEquals("816a33df",
                PlaySceneView.shortRoomId("match-816a33df-6e2a-4772-b3e7-09c6768d4150"));
    }

    @Test
    void shortRoomId_createOrJoinShortCode_isUnchanged() {
        assertEquals("AB12CD", PlaySceneView.shortRoomId("AB12CD"));
    }

    @Test
    void shortRoomId_defaultRoom_isUnchanged() {
        assertEquals("default", PlaySceneView.shortRoomId("default"));
    }

    @Test
    void shortRoomId_nullGameId_returnsQuestionMark() {
        assertEquals("?", PlaySceneView.shortRoomId(null));
    }

    @Test
    void shortRoomId_matchmakingIdShorterThan8Chars_returnsWhateverRemains() {
        // Theoretical edge case (a real UUID is always far longer than 8): it must not
        // throw even if the part after "match-" is shorter than expected.
        assertEquals("ab", PlaySceneView.shortRoomId("match-ab"));
    }
}
