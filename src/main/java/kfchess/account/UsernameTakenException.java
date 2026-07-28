package kfchess.account;


/** Thrown by {@link AccountRepository#register} when the chosen username is already registered. */
public class UsernameTakenException extends Exception {

    public UsernameTakenException(String username) {
        super("username already taken: " + username);
    }
}
