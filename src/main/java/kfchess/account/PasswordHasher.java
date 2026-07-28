package kfchess.account;

import org.mindrot.jbcrypt.BCrypt;


/** Thin wrapper around jBCrypt (salt is managed automatically inside the hash). */
public final class PasswordHasher {

    // Utility class only - no instances needed.
    private PasswordHasher() {
    }

    /** Hashes a raw password with a fresh random salt. */
    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /** Checks a raw password against a previously stored hash. */
    public static boolean matches(String rawPassword, String hash) {
        return BCrypt.checkpw(rawPassword, hash);
    }
}
