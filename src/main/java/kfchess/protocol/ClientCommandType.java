package kfchess.protocol;


/** The message types a client can send to the server; maps directly to the JSON "type" field. */
public enum ClientCommandType {
    CLICK,
    JUMP,
    /** Asks to reset the board - GameSession only honors it after the game ends and both sides request it. */
    RESTART
}
