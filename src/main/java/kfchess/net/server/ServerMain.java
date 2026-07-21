package kfchess.net.server;

/**
 * נקודת הכניסה להרצת השרת בפועל. פורט ברירת מחדל 8887, ניתן לשינוי
 * כפרמטר ראשון לשורת הפקודה (למשל להרצת כמה שרתים מקבילים לבדיקות).
 */
public final class ServerMain {

    private static final int DEFAULT_PORT = 8887;

    private ServerMain() {
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new GameServer(port).start();
    }
}
