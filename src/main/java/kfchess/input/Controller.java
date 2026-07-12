package kfchess.input;

import java.util.Objects;
import java.util.regex.Pattern;

public class Controller {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String PRINT_BOARD_ARGUMENT = "board";

    public enum CommandType {
        CLICK, WAIT, JUMP, PRINT_BOARD, UNKNOWN
    }

    public static final class Command {
        private final CommandType type;
        private final int x;
        private final int y;
        private final long milliseconds;

        public Command(CommandType type, int x, int y, long milliseconds) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.milliseconds = milliseconds;
        }

        public CommandType type() { return type; }
        public int x() { return x; }
        public int y() { return y; }
        public long milliseconds() { return milliseconds; }

        public static Command click(int x, int y) {
            return new Command(CommandType.CLICK, x, y, 0);
        }

        public static Command waitFor(long milliseconds) {
            return new Command(CommandType.WAIT, 0, 0, milliseconds);
        }

        public static Command jump(int x, int y) {
            return new Command(CommandType.JUMP, x, y, 0);
        }

        public static Command printBoard() {
            return new Command(CommandType.PRINT_BOARD, 0, 0, 0);
        }

        public static Command unknown() {
            return new Command(CommandType.UNKNOWN, 0, 0, 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Command command = (Command) o;
            return x == command.x && y == command.y && milliseconds == command.milliseconds && type == command.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, x, y, milliseconds);
        }
    }

    public Command parse(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (line.isEmpty()) {
            return Command.unknown();
        }

        String[] parts = WHITESPACE.split(line);
        String commandName = parts[0].toLowerCase();

        try {
            return parseKnownCommand(commandName, parts);
        } catch (NumberFormatException invalidNumber) {
            return Command.unknown();
        }
    }

    private Command parseKnownCommand(String commandName, String[] parts) {
        switch (commandName) {
            case "click":
                if (parts.length == 3) {
                    return Command.click(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }
                break;
            case "wait":
                if (parts.length == 2) {
                    return Command.waitFor(Long.parseLong(parts[1]));
                }
                break;
            case "jump":
                if (parts.length == 3) {
                    return Command.jump(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }
                break;
            case "print":
                if (parts.length == 2 && parts[1].equalsIgnoreCase(PRINT_BOARD_ARGUMENT)) {
                    return Command.printBoard();
                }
                break;
            default:
                break;
        }
        return Command.unknown();
    }
}