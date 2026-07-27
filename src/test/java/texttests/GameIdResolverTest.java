package texttests;

import kfchess.server.GameIdResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameIdResolverTest {

    @Test
    void resolve_pathWithGameId_stripsSlashes() {
        assertEquals("room1", GameIdResolver.resolve("/room1"));
    }

    @Test
    void resolve_rootPath_returnsDefault() {
        assertEquals("default", GameIdResolver.resolve("/"));
    }

    @Test
    void resolve_emptyPath_returnsDefault() {
        assertEquals("default", GameIdResolver.resolve(""));
    }

    @Test
    void resolve_nullPath_returnsDefault() {
        assertEquals("default", GameIdResolver.resolve(null));
    }

    @Test
    void resolve_pathWithTrailingSlash_stripsIt() {
        assertEquals("room1", GameIdResolver.resolve("/room1/"));
    }

    @Test
    void resolve_pathWithQueryString_stripsQuery() {
        // שלב 4 Part B: ?username= מגיע על אותו URI - בלי חיתוך query
        // string זה היה הופך בטעות לחלק משם ה-room.
        assertEquals("room1", GameIdResolver.resolve("/room1?username=ruth"));
    }

    @Test
    void resolve_rootPathWithQueryString_returnsDefault() {
        assertEquals("default", GameIdResolver.resolve("/?username=ruth"));
    }
}
