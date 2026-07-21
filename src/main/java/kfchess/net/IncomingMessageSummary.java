package kfchess.net;

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

    // קוראת רק את שדה "type" ומפיקה שורה מתאימה; הודעה לא מזוהה/פגומה מקבלת שורה גנרית ולא זורקת חריגה.
    public static String describe(String json) {
        JsonObject message = JsonParser.parseString(json).getAsJsonObject();
        String type = message.has("type") ? message.get("type").getAsString() : "UNKNOWN";
        return switch (type) {
            case "ROLE_ASSIGNED" -> "[ROLE_ASSIGNED] role=" + message.get("role").getAsString()
                    + " gameId=" + message.get("gameId").getAsString();
            case "SNAPSHOT" -> "[SNAPSHOT] now=" + message.get("now").getAsLong()
                    + " pieces=" + message.getAsJsonArray("pieces").size()
                    + " gameOver=" + message.get("gameOver").getAsBoolean();
            case "ERROR" -> "[ERROR] " + message.get("message").getAsString();
            default -> "[UNKNOWN] " + json;
        };
    }
}
