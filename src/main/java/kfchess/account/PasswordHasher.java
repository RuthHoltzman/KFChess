package kfchess.account;

import org.mindrot.jbcrypt.BCrypt;


public final class PasswordHasher {

    private PasswordHasher() {
        // מחלקת utility בלבד - אין טעם ליצור ממנה מופע
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }


    public static boolean matches(String rawPassword, String hash) {
        return BCrypt.checkpw(rawPassword, hash);
    }
}
