package kfchess.account;

import java.util.Optional;


/** Storage for player accounts: registration, login, and ELO lookups/updates. */
public interface AccountRepository {

    /** Creates a new account with the starting ELO. Fails if the username is already taken. */
    Account register(String username, String rawPassword) throws UsernameTakenException;

    /** Authenticates a username/password pair; empty if either is wrong (no user-enumeration hint). */
    Optional<Account> login(String username, String rawPassword);

    /** Current ELO for a username, or empty if the account doesn't exist. */
    Optional<Integer> currentElo(String username);

    /** Overwrites the stored ELO for a username. */
    void updateElo(String username, int newElo);
}
