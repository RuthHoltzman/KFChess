package kfchess.server.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * מחלץ את פרמטר ה-username מה-query string של נתיב החיבור (למשל
 * "/room1?username=ruth" -> "ruth") - שלב 4 Part B: חיבור ה-username
 * המחובר (מ-LoginScreenMain) לפרוטוקול הרשת עצמו, כדי ש-GameSession
 * ידע למי לעדכן ELO בסוף משחק (ר' GameSession.onGameLifecycleEvent).
 * <p>
 * מחלקה נפרדת מ-GameIdResolver (לא הרחבה שלו) בכוונה - זה query
 * parameter נפרד לגמרי מנתיב ה-room, לא אותו פענוח. חיבור בלי username
 * בכלל (למשל בדיקת פרוטוקול גולמי מקונסולת דפדפן, בלי login) הוא מקרה
 * נתמך לגמרי - Optional.empty(), לא שגיאה.
 */
public final class UsernameResolver {

    private static final String USERNAME_PARAM = "username";

    private UsernameResolver() {
    }

    public static Optional<String> resolve(String resourceDescriptor) {
        if (resourceDescriptor == null) {
            return Optional.empty();
        }
        int queryStart = resourceDescriptor.indexOf('?');
        if (queryStart < 0 || queryStart == resourceDescriptor.length() - 1) {
            return Optional.empty();
        }
        String query = resourceDescriptor.substring(queryStart + 1);
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(USERNAME_PARAM) && !parts[1].isBlank()) {
                return Optional.of(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }
}
