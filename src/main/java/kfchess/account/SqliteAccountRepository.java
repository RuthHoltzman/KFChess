package kfchess.account;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;


/** SQLite-backed {@link AccountRepository} - one row per username, password hash + ELO. */
public class SqliteAccountRepository implements AccountRepository {

    private static final int STARTING_ELO = 1200;

    public static final String DEFAULT_DB_FILE = "kfchess.db";

    private final String jdbcUrl;

    /** Opens (and creates if missing) the accounts table in the given SQLite file. */
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

    /** Inserts a new account at the starting ELO; throws if the username already exists. */
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
            if (isUniqueConstraintViolation(insertFailed)) {
                throw new UsernameTakenException(username);
            }
            throw new IllegalStateException("failed to register account", insertFailed);
        }
    }

    /** Looks up the account and checks the password hash. */
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


    /** True if the SQL failure was a duplicate-primary-key error (SQLite error code 19). */
    private boolean isUniqueConstraintViolation(SQLException ex) {
        return ex.getErrorCode() == 19;
    }

    @Override
    public Optional<Integer> currentElo(String username) {
        String sql = "SELECT elo FROM accounts WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(result.getInt("elo"));
            }
        } catch (SQLException queryFailed) {
            throw new IllegalStateException("failed to query elo for " + username, queryFailed);
        }
    }

    /** Overwrites the stored ELO for an existing account. */
    @Override
    public void updateElo(String username, int newElo) {
        String sql = "UPDATE accounts SET elo = ? WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newElo);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException updateFailed) {
            throw new IllegalStateException("failed to update elo for " + username, updateFailed);
        }
    }
}
