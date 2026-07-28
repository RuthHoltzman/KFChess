package kfchess.server;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsernameResolverTest {

    @Test
    void resolve_pathWithUsername_returnsIt() {
        assertEquals(Optional.of("ruth"), UsernameResolver.resolve("/room1?username=ruth"));
    }

    @Test
    void resolve_pathWithoutQueryString_returnsEmpty() {
        assertEquals(Optional.empty(), UsernameResolver.resolve("/room1"));
    }

    @Test
    void resolve_nullPath_returnsEmpty() {
        assertEquals(Optional.empty(), UsernameResolver.resolve(null));
    }

    @Test
    void resolve_queryStringWithoutUsernameParam_returnsEmpty() {
        assertEquals(Optional.empty(), UsernameResolver.resolve("/room1?other=value"));
    }

    @Test
    void resolve_emptyUsernameValue_returnsEmpty() {
        assertEquals(Optional.empty(), UsernameResolver.resolve("/room1?username="));
    }

    @Test
    void resolve_usernameWithUrlEncodedCharacters_decodesIt() {
        assertEquals(Optional.of("ruth h"), UsernameResolver.resolve("/room1?username=ruth%20h"));
    }

    @Test
    void resolve_multipleQueryParams_findsUsernameAmongThem() {
        assertEquals(Optional.of("ruth"), UsernameResolver.resolve("/room1?other=value&username=ruth"));
    }
}
