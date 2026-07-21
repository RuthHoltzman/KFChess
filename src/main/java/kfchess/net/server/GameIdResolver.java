package kfchess.net.server;

/**
 * הופך את נתיב החיבור (מהבקשה ההתחלתית של ה-WebSocket) ל-gameId: מחלקה
 * קטנה ונפרדת (ולא מתודה פרטית בתוך GameServer) בכוונה, כדי שאפשר יהיה
 * לבדוק את הלוגיקה הטהורה הזו ביחידה בלי להרים שרת/handshake מזויף.
 */
public final class GameIdResolver {

    private static final String DEFAULT_GAME_ID = "default";

    private GameIdResolver() {
    }

    // "/room1" -> "room1"; שורש ("/" או ריק או null) -> ברירת מחדל, כדי לתמוך בכמה משחקים בלי UI לחדרים עדיין.
    public static String resolve(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return DEFAULT_GAME_ID;
        }
        String trimmed = resourceDescriptor.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.isEmpty() ? DEFAULT_GAME_ID : trimmed;
    }
}
