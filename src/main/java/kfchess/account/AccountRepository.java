package kfchess.account;

import java.util.Optional;


public interface AccountRepository {

    Account register(String username, String rawPassword) throws UsernameTakenException;


    Optional<Account> login(String username, String rawPassword);

    Optional<Integer> currentElo(String username);

    void updateElo(String username, int newElo);
}
