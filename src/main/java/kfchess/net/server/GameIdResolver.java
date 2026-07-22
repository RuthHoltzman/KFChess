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
    // חותך גם query string אם יש (למשל "/room1?username=ruth" -> "room1") -
    // שלב 4 Part B הוסיף ?username= לאותו URI, ובלי החיתוך הזה הוא היה
    // "נדבק" בטעות לתוך שם ה-room עצמו.
    public static String resolve(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return DEFAULT_GAME_ID;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.isEmpty() ? DEFAULT_GAME_ID : trimmed;
    }
}
