package kfchess.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchmakingResolverTest {

    @Test
    void isMatchmakingRequest_matchmakingPath_returnsTrue() {
        assertTrue(MatchmakingResolver.isMatchmakingRequest("/_play"));
    }

    @Test
    void isMatchmakingRequest_matchmakingPathWithUsername_returnsTrue() {
        // Same reasoning as PlayIdResolverTest.resolve_pathWithQueryString_stripsQuery -
        // ?username= must be ignored when matching the path.
        assertTrue(MatchmakingResolver.isMatchmakingRequest("/_play?username=ruth"));
    }

    @Test
    void isMatchmakingRequest_matchmakingPathWithTrailingSlash_returnsTrue() {
        assertTrue(MatchmakingResolver.isMatchmakingRequest("/_play/"));
    }

    @Test
    void isMatchmakingRequest_namedRoom_returnsFalse() {
        assertFalse(MatchmakingResolver.isMatchmakingRequest("/room1"));
    }

    @Test
    void isMatchmakingRequest_rootPath_returnsFalse() {
        assertFalse(MatchmakingResolver.isMatchmakingRequest("/"));
    }

    @Test
    void isMatchmakingRequest_nullPath_returnsFalse() {
        assertFalse(MatchmakingResolver.isMatchmakingRequest(null));
    }
}
