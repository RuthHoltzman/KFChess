package kfchess.account;

import org.mindrot.jbcrypt.BCrypt;

/**
 * עטיפה דקה סביב ספריית jBCrypt - כל שאר הקוד (SqliteAccountRepository,
 * טסטים) קורא רק ל-hash()/matches() ולא ל-BCrypt ישירות, כדי שאם אי-פעם
 * נחליף ספריית גיבוב, רק הקובץ הזה משתנה. jBCrypt נבחר (במקום PBKDF2
 * מובנה ב-Java) כי ה-salt מנוהל אוטומטית בתוך מחרוזת ה-hash עצמה - פחות
 * קוד תשתית סביב אבטחת הסיסמאות, פחות מקום לטעות.
 */
public final class PasswordHasher {

    private PasswordHasher() {
        // מחלקת utility בלבד - אין טעם ליצור ממנה מופע
    }

    // מייצר hash חדש (עם salt אקראי חדש) מסיסמה גולמית - נקרא פעם אחת ב-register.
    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    // בודק אם סיסמה גולמית תואמת ל-hash שכבר שמור - נקרא ב-login, בלי
    // לשחזר את הסיסמה המקורית מה-hash בשום שלב (זה כל הרעיון ב-hashing).
    public static boolean matches(String rawPassword, String hash) {
        return BCrypt.checkpw(rawPassword, hash);
    }
}
