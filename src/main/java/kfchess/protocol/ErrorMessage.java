package kfchess.protocol;


/** Outgoing-only DTO sent when a client message is malformed or its command can't be applied. */
public class ErrorMessage {

    private final String type = "ERROR";
    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }
}
