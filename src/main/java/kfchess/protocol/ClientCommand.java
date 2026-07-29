package kfchess.protocol;

/** Two-way DTO for a click/jump/restart command; row/col are board coordinates, not pixels. */
public record ClientCommand(ClientCommandType type, Integer row, Integer col) {

    /** Basic sanity check before converting to a Position - RESTART needs no row/col at all. */
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        return type == ClientCommandType.RESTART || (row != null && col != null);
    }
}
