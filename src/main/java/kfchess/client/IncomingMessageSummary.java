package kfchess.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/** Turns a raw server JSON message into a short, human-readable summary line for console/log output. */
public final class IncomingMessageSummary {

    private IncomingMessageSummary() {
    }

    /** Reads the "type" field from a raw JSON message, or "UNKNOWN" if missing. */
    public static String messageType(String json) {
        JsonObject message = JsonParser.parseString(json).getAsJsonObject();
        return message.has("type") ? message.get("type").getAsString() : "UNKNOWN";
    }

    /** True if this message is a SNAPSHOT (the frequent, high-volume type). */
    public static boolean isSnapshot(String json) {
        return "SNAPSHOT".equals(messageType(json));
    }

    /** True if this message is the one-time ROLE_ASSIGNED message sent right after connecting. */
    public static boolean isRoleAssigned(String json) {
        return "ROLE_ASSIGNED".equals(messageType(json));
    }

    /** True if this message is the one-time MATCHMAKING_TIMEOUT message. */
    public static boolean isMatchmakingTimeout(String json) {
        return "MATCHMAKING_TIMEOUT".equals(messageType(json));
    }

    /** Builds a one-line human-readable description of any message type. */
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

    /** Formats the "selected" position for describe(), or "none" if absent. */
    private static String describeSelected(JsonObject message) {
        if (!message.has("selected")) {
            return "none";
        }
        JsonObject selected = message.getAsJsonObject("selected");
        return "(" + selected.get("row").getAsInt() + "," + selected.get("col").getAsInt() + ")";
    }
}
