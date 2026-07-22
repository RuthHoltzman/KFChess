package texttests;

import kfchess.account.Account;
import kfchess.account.SqliteAccountRepository;
import kfchess.account.UsernameTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// @TempDir נותן קובץ/תיקייה זמניים לכל טסט - כדי שהטסטים ירוצו על קובץ
// SQLite נפרד לגמרי מ-kfchess.db האמיתי (זה שנוצר כשמריצים את המשחק),
// וכדי שטסטים לא ישפיעו אחד על השני.
class SqliteAccountRepositoryTest {

    private SqliteAccountRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        String dbFile = tempDir.resolve("test-accounts.db").toString();
        repository = new SqliteAccountRepository(dbFile);
    }

    @Test
    void register_newUsername_returnsAccountWithStartingElo() throws UsernameTakenException {
        Account account = repository.register("ruth", "s3cret");
        assertEquals("ruth", account.username());
        assertEquals(1200, account.elo());
    }

    @Test
    void register_duplicateUsername_throwsUsernameTakenException() throws UsernameTakenException {
        repository.register("ruth", "s3cret");
        assertThrows(UsernameTakenException.class, () -> repository.register("ruth", "another-password"));
    }

    @Test
    void login_correctCredentials_returnsAccount() throws UsernameTakenException {
        repository.register("ruth", "s3cret");
        Optional<Account> account = repository.login("ruth", "s3cret");
        assertTrue(account.isPresent());
        assertEquals("ruth", account.get().username());
    }

    @Test
    void login_wrongPassword_returnsEmpty() throws UsernameTakenException {
        repository.register("ruth", "s3cret");
        assertEquals(Optional.empty(), repository.login("ruth", "wrong-password"));
    }

    @Test
    void login_unknownUsername_returnsEmpty() {
        assertEquals(Optional.empty(), repository.login("no-such-user", "whatever"));
    }

    @Test
    void currentElo_existingAccount_returnsStartingElo() throws UsernameTakenException {
        repository.register("ruth", "s3cret");
        assertEquals(Optional.of(1200), repository.currentElo("ruth"));
    }

    @Test
    void currentElo_unknownUsername_returnsEmpty() {
        assertEquals(Optional.empty(), repository.currentElo("no-such-user"));
    }

    @Test
    void updateElo_existingAccount_changesCurrentElo() throws UsernameTakenException {
        repository.register("ruth", "s3cret");
        repository.updateElo("ruth", 1216);
        assertEquals(Optional.of(1216), repository.currentElo("ruth"));
    }

    @Test
    void updateElo_doesNotAffectOtherAccounts() throws UsernameTakenException {
        repository.register("ruth", "s3cret");
        repository.register("dani", "another-password");

        repository.updateElo("ruth", 1216);

        assertEquals(Optional.of(1200), repository.currentElo("dani"));
    }
}
