package kfchess.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * הופך הודעת JSON גולמית שמגיעה מהשרת לשורת טקסט קריאה אחת - כדי
 * שהלקוח (GameClient) לא יציף את המסוף עם snapshot מלא כ-30 פעם
 * בשנייה (בדיוק מה שקרה בבדיקה הידנית מהדפדפן). לוגיקה טהורה, לא
 * תלויה בשום חיבור רשת - ר' GameIdResolver לאותו רעיון בצד השרת.
 */
public final class IncomingMessageSummary {

    private IncomingMessageSummary() {
    }

    // "type" גולמי מתוך ה-JSON, בלי לבנות שורה מלאה - נחוץ ל-GameClient כדי להחליט אם להדפיס מיד או לשמור בשקט.
    public static String messageType(String json) {
        JsonObject message = JsonParser.parseString(json).getAsJsonObject();
        return message.has("type") ? message.get("type").getAsString() : "UNKNOWN";
    }

    // true אם זו הודעת SNAPSHOT - אלה שמגיעות ברצף מהיר (כ-30/שנייה) ולא כדאי להדפיס אוטומטית לקונסולה.
    public static boolean isSnapshot(String json) {
        return "SNAPSHOT".equals(messageType(json));
    }

    // true אם זו הודעת ROLE_ASSIGNED - נשלחת פעם אחת בלבד, מיד אחרי החיבור
    // (ר' RoleAssignedMessage). GameClient משתמש בזה כדי לדעת מתי לחלץ את
    // ה-gameId בפועל (שלב 6 - "Create room": השרת ממציא אותו, הלקוח לא
    // יודע אותו מראש בכלל).
    public static boolean isRoleAssigned(String json) {
        return "ROLE_ASSIGNED".equals(messageType(json));
    }

    // true אם זו הודעת MATCHMAKING_TIMEOUT - תיקון "Play" לפי המפרט המדויק
    // (ר' MatchmakingTimeoutMessage/GameServer.checkMatchmakingTimeout):
    // נשלחת פעם אחת בלבד, כשעברה דקה בלי יריב/ה עם ELO תואם. GameClient
    // שומר אותה כדי ש-NetworkGameWindow יציג popup ויסגור את החלון.
    public static boolean isMatchmakingTimeout(String json) {
        return "MATCHMAKING_TIMEOUT".equals(messageType(json));
    }

    // קוראת רק את שדה "type" ומפיקה שורה מתאימה; הודעה לא מזוהה/פגומה מקבלת שורה גנרית ולא זורקת חריגה.
    public static String describe(String json) {
        JsonObject message = JsonParser.parseString(json).getAsJsonObject();
        String type = message.has("type") ? message.get("type").getAsString() : "UNKNOWN";
        return switch (type) {
            case "ROLE_ASSIGNED" -> "[ROLE_ASSIGNED] role=" + message.get("role").getAsString()
                    + " gameId=" + message.get("gameId").getAsString();
            case "SNAPSHOT" -> "[SNAPSHOT] now=" + message.get("now").getAsLong()
                    + " pieces=" + message.getAsJsonArray("pieces").size()
                    + " gameOver=" + message.get("gameOver").getAsBoolean()
                    + " selected=" + describeSelected(message);
            case "ERROR" -> "[ERROR] " + message.get("message").getAsString();
            case "MATCHMAKING_TIMEOUT" -> "[MATCHMAKING_TIMEOUT] " + message.get("message").getAsString();
            default -> "[UNKNOWN] " + json;
        };
    }

    // "selected" הוא null-אבל-מושמט (Gson לא שולח שדה null כברירת מחדל) - has() בודק בדיוק את זה.
    private static String describeSelected(JsonObject message) {
        if (!message.has("selected")) {
            return "none";
        }
        JsonObject selected = message.getAsJsonObject("selected");
        return "(" + selected.get("row").getAsInt() + "," + selected.get("col").getAsInt() + ")";
    }
}
