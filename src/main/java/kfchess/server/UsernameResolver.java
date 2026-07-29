package kfchess.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Extracts the username query parameter from a connection path ("/room1?username=ruth" -&gt; "ruth"),
 * so the session knows whose ELO to update when the game ends.
 * <p>
 * Kept separate from GameIdResolver because this is a query parameter, not part of the room path.
 * A connection with no username at all is fully supported - it returns empty, not an error.
 */
public final class UsernameResolver {

    private static final String USERNAME_PARAM = "username";

    private UsernameResolver() {
    }

    /** The decoded username from the path's query string, or empty if there isn't one. */
    public static Optional<String> resolve(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return Optional.empty();
        }
        int queryStart = resourceDescriptor.indexOf('?');
        if (queryStart < 0 || queryStart == resourceDescriptor.length() - 1) {
            return Optional.empty();
        }
        String query = resourceDescriptor.substring(queryStart + 1);
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(USERNAME_PARAM) && !parts[1].isBlank()) {
                return Optional.of(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }
}
