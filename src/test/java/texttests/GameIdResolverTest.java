package texttests;

import kfchess.net.server.GameIdResolver;
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
}
