package kfchess.server.server;

/**
 * מזהה אם בקשת חיבור WebSocket היא בקשת matchmaking (כפתור "Skip" ב-
 * HomeScreenMain, שלב 5 חלק 2) ולא חיבור לחדר-בשם ספציפי - לפי נתיב
 * שמור (MATCHMAKING_PATH). מחלקה טהורה ונפרדת (לא בדיקה inline בתוך
 * GameServer.onOpen), בדיוק מאותה סיבה ש-GameIdResolver כבר קיימת ככה:
 * אפשר לבדוק את הלוגיקה ביחידה בלי להרים שרת/handshake מזויף.
 */
public final class MatchmakingResolver {

    // הטוקן חייב להיות זהה בדיוק לזה שב-HomeScreenMain.buildMatchmakingUri
    // בצד הלקוח - שני הקצוות מגדירים אותו בנפרד (כמו ש-"default"/DEFAULT_ROOM
    // כבר מוגדר בנפרד היום ב-GameIdResolver וב-HomeScreenMain), לא משותף
    // ע"י מחלקת קבועים אחת. מגבלה ידועה: מי שמקלידה "_play" כשם room ידני
    // "תתנגש" בטעות עם matchmaking - נדיר מספיק בפועל, לא טופל.
    private static final String MATCHMAKING_PATH = "_play";

    private MatchmakingResolver() {
    }

    // אותה שיטת חיתוך query string בדיוק כמו GameIdResolver.resolve, כדי
    // ש-"/_play?username=ruth" יזוהה נכון בדיוק כמו "/_play" הפשוט.
    public static boolean isMatchmakingRequest(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return false;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.equals(MATCHMAKING_PATH);
    }
}
