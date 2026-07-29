package kfchess.server;

import java.security.SecureRandom;

/**
 * Generates a short, human-readable room code - deliberately not a UUID, because this code gets read
 * off the screen and passed between people so a friend can Join.
 * <p>
 * 6 characters from A-Z and 0-9, about 2 billion combinations, so collisions are very rare
 * (GameServer still checks and retries if one happens).
 */
public final class RoomIdGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomIdGenerator() {
    }

    /** A new random 6-character room code. */
    public static String generate() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
