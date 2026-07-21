package kfchess.net.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * נקודת כניסה לבדיקה ידנית של GameClient מטרמינל רגיל, בלי דפדפן -
 * מתחברת (וממתינה שההתחברות תושלם), ואז קוראת שורות פקודה פשוטות:
 * "click ROW COL", "jump ROW COL", "quit".
 */
public final class ClientMain {

    private static final String DEFAULT_SERVER_URI = "ws://localhost:8887/default";

    private ClientMain() {
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        String serverUri = args.length > 0 ? args[0] : DEFAULT_SERVER_URI;
        GameClient client = new GameClient(new URI(serverUri));

        boolean connected = client.connectBlocking();
        if (!connected) {
            System.err.println("failed to connect to " + serverUri);
            return;
        }

        readCommandsFromConsole(client);
    }

    // לולאת קלט פשוטה: מפרקת כל שורה למילה ראשונה (הפקודה) ושני מספרים (row/col), עד "quit".
    private static void readCommandsFromConsole(GameClient client) {
        Scanner console = new Scanner(System.in);
        System.out.println("commands: click ROW COL | jump ROW COL | status | quit");
        while (console.hasNextLine()) {
            String line = console.nextLine().trim();
            if (line.equalsIgnoreCase("quit")) {
                break;
            }
            handleLine(client, line);
        }
    }

    private static void handleLine(GameClient client, String line) {
        String[] parts = line.split("\\s+");
        // "status" בלי ארגומנטים - מדפיס את ה-snapshot האחרון לפי דרישה, במקום שיוצף אוטומטית.
        if (parts.length == 1 && parts[0].equalsIgnoreCase("status")) {
            client.printLatestSnapshot();
            return;
        }
        if (parts.length != 3) {
            System.out.println("expected: click/jump ROW COL, or: status");
            return;
        }
        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            if (parts[0].equalsIgnoreCase("click")) {
                client.sendClick(row, col);
            } else if (parts[0].equalsIgnoreCase("jump")) {
                client.sendJump(row, col);
            } else {
                System.out.println("unknown command: " + parts[0]);
            }
        } catch (NumberFormatException e) {
            System.out.println("row/col must be numbers");
        }
    }
}
