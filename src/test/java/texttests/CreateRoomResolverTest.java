package texttests;

import kfchess.server.server.CreateRoomResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateRoomResolverTest {

    @Test
    void isCreateRoomRequest_createPath_returnsTrue() {
        assertTrue(CreateRoomResolver.isCreateRoomRequest("/_create"));
    }

    @Test
    void isCreateRoomRequest_createPathWithUsername_returnsTrue() {
        assertTrue(CreateRoomResolver.isCreateRoomRequest("/_create?username=ruth"));
    }

    @Test
    void isCreateRoomRequest_createPathWithTrailingSlash_returnsTrue() {
        assertTrue(CreateRoomResolver.isCreateRoomRequest("/_create/"));
    }

    @Test
    void isCreateRoomRequest_namedRoom_returnsFalse() {
        assertFalse(CreateRoomResolver.isCreateRoomRequest("/room1"));
    }

    @Test
    void isCreateRoomRequest_matchmakingPath_returnsFalse() {
        // חייב להישאר עצמאי מ-MatchmakingResolver - שני נתיבים שמורים שונים.
        assertFalse(CreateRoomResolver.isCreateRoomRequest("/_play"));
    }

    @Test
    void isCreateRoomRequest_nullPath_returnsFalse() {
        assertFalse(CreateRoomResolver.isCreateRoomRequest(null));
    }
}
