package kfchess.server;

import java.security.SecureRandom;

/**
 * מייצרת קוד room קצר וקריא-לבני-אדם (שלב 6, כפתור "Create") - בכוונה
 * *לא* UUID כמו ש-GameServer.resolveMatchmakingGameId כבר עושה: קוד
 * ה-room הזה אמור להיכתב "בראש המסך" ולהיאמר/להיכתב בין אנשים כדי
 * שחברה תוכל להצטרף (Join) - UUID ארוך מדי לזה בפועל.
 * <p>
 * 6 תווים, אותיות גדולות (A-Z) + ספרות (0-9) - ~2 מיליארד צירופים
 * אפשריים, מספיק כדי שהתנגשות תהיה נדירה ביותר (GameServer עדיין בודק
 * ומנסה שוב אם בכל זאת קרתה - ר' createNewRoomGameId).
 */
public final class RoomIdGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomIdGenerator() {
    }

    public static String generate() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
