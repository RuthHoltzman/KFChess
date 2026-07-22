package texttests;

import kfchess.account.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hash_correctPassword_matches() {
        String hash = PasswordHasher.hash("s3cret");
        assertTrue(PasswordHasher.matches("s3cret", hash));
    }

    @Test
    void hash_wrongPassword_doesNotMatch() {
        String hash = PasswordHasher.hash("s3cret");
        assertFalse(PasswordHasher.matches("wrong-password", hash));
    }

    @Test
    void hash_samePasswordTwice_producesDifferentHashes() {
        // כל hash מקבל salt אקראי חדש - כדי שלא יהיה אפשר להשוות טבלת
        // hash-ים בין חשבונות שונים עם אותה סיסמה (rainbow table).
        String firstHash = PasswordHasher.hash("s3cret");
        String secondHash = PasswordHasher.hash("s3cret");
        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.matches("s3cret", firstHash));
        assertTrue(PasswordHasher.matches("s3cret", secondHash));
    }
}
