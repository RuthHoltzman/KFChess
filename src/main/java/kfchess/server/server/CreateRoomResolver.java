package kfchess.server.server;

/**
 * מזהה אם בקשת חיבור WebSocket היא בקשת "Create room" (כפתור Create
 * בדיאלוג ה-Room, שלב 6) - לפי נתיב שמור (CREATE_ROOM_PATH), באותו
 * דפוס בדיוק כמו MatchmakingResolver (וגם GameIdResolver לפני זה):
 * מחלקה טהורה נפרדת, נבדקת ביחידה בלי handshake מזויף.
 */
public final class CreateRoomResolver {

    // חייב להיות זהה בדיוק לטוקן שקבוע ב-HomeScreenMain.buildCreateRoomUri
    // בצד הלקוח - שני הקצוות מגדירים אותו בנפרד, אותו עיקרון בדיוק כמו
    // MATCHMAKING_PATH/"_play".
    private static final String CREATE_ROOM_PATH = "_create";

    private CreateRoomResolver() {
    }

    // אותה שיטת חיתוך query string בדיוק כמו GameIdResolver/MatchmakingResolver.
    public static boolean isCreateRoomRequest(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return false;
        }
        String pathOnly = resourceDescriptor.split("\\?", 2)[0];
        String trimmed = pathOnly.replaceAll("^/+", "").replaceAll("/+$", "");
        return trimmed.equals(CREATE_ROOM_PATH);
    }
}
