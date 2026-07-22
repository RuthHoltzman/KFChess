package kfchess.account;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * מימוש AccountRepository על גבי קובץ SQLite אחד (טבלה יחידה `accounts`).
 * פותחת חיבור JDBC חדש בכל קריאה (register/login) ולא מחזיקה חיבור
 * פתוח קבוע - קובץ SQLite מקומי נפתח/נסגר מהר מספיק, וכך נמנעים
 * מבעיות concurrency סביב חיבור משותף בין threads בלי לסבך את המחלקה
 * בפול (pool) שלא צריך בסדר גודל הזה.
 */
public class SqliteAccountRepository implements AccountRepository {

    private static final int STARTING_ELO = 1200;

    private final String jdbcUrl;

    // dbFilePath, למשל "kfchess.db" - נוצר כקובץ יחסי לתיקיית ההרצה. יוצר
    // את הטבלה מיד אם היא לא קיימת עדיין, כדי שכל שאר המחלקה תוכל להניח
    // בבטחה שהיא כבר שם.
    public SqliteAccountRepository(String dbFilePath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
        createTableIfMissing();
    }

    private void createTableIfMissing() {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "username TEXT PRIMARY KEY, " +
                "password_hash TEXT NOT NULL, " +
                "elo INTEGER NOT NULL)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException creationFailed) {
            throw new IllegalStateException("failed to initialize accounts table", creationFailed);
        }
    }

    @Override
    public Account register(String username, String rawPassword) throws UsernameTakenException {
        String hash = PasswordHasher.hash(rawPassword);
        String sql = "INSERT INTO accounts (username, password_hash, elo) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, hash);
            statement.setInt(3, STARTING_ELO);
            statement.executeUpdate();
            return new Account(username, STARTING_ELO);
        } catch (SQLException insertFailed) {
            // PRIMARY KEY על username - הדרך הסטנדרטית לזהות "כבר קיים" היא
            // לתפוס את הכשלון מה-DB עצמו (constraint violation) במקום
            // SELECT-then-INSERT נפרד, שיש בו חלון race בין שני threads.
            if (isUniqueConstraintViolation(insertFailed)) {
                throw new UsernameTakenException(username);
            }
            throw new IllegalStateException("failed to register account", insertFailed);
        }
    }

    @Override
    public Optional<Account> login(String username, String rawPassword) {
        String sql = "SELECT password_hash, elo FROM accounts WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String storedHash = result.getString("password_hash");
                if (!PasswordHasher.matches(rawPassword, storedHash)) {
                    return Optional.empty();
                }
                return Optional.of(new Account(username, result.getInt("elo")));
            }
        } catch (SQLException queryFailed) {
            throw new IllegalStateException("failed to query account", queryFailed);
        }
    }

    // מזהה "username כבר קיים" לפי קוד השגיאה הסטנדרטי של SQLite ל-UNIQUE/
    // PRIMARY KEY constraint (SQLITE_CONSTRAINT = 19), בלי להסתמך על טקסט
    // הודעת השגיאה (שיכול להשתנות בין גרסאות דרייבר).
    private boolean isUniqueConstraintViolation(SQLException ex) {
        return ex.getErrorCode() == 19;
    }
}
