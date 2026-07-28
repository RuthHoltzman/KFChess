package kfchess.account;


/** A logged-in player's public profile - everything allowed to leave the repository (no password hash). */
public record Account(String username, int elo) {
}
