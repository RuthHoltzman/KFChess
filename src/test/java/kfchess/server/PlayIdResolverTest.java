package kfchess.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayIdResolverTest {

    @Test
    void resolve_pathWithGameId_stripsSlashes() {
        assertEquals("room1", PlayIdResolver.resolve("/room1"));
    }

    @Test
    void resolve_rootPath_returnsDefault() {
        assertEquals("default", PlayIdResolver.resolve("/"));
    }

    @Test
    void resolve_emptyPath_returnsDefault() {
        assertEquals("default", PlayIdResolver.resolve(""));
    }

    @Test
    void resolve_nullPath_returnsDefault() {
        assertEquals("default", PlayIdResolver.resolve(null));
    }

    @Test
    void resolve_pathWithTrailingSlash_stripsIt() {
        assertEquals("room1", PlayIdResolver.resolve("/room1/"));
    }

    @Test
    void resolve_pathWithQueryString_stripsQuery() {
        // ?username= arrives on the same URI - without stripping the query string
        // it would end up as part of the room name.
        assertEquals("room1", PlayIdResolver.resolve("/room1?username=ruth"));
    }

    @Test
    void resolve_rootPathWithQueryString_returnsDefault() {
        assertEquals("default", PlayIdResolver.resolve("/?username=ruth"));
    }
}
