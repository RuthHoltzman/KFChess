package kfchess.account;


public class UsernameTakenException extends Exception {

    public UsernameTakenException(String username) {
        super("username already taken: " + username);
    }
}
