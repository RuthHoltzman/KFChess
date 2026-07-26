package kfchess.account;

import java.util.Optional;

/**
 * ממשק לניהול חשבונות - מפריד את הלוגיקה (מי מותר לו להתחבר, מי כבר
 * קיים) מהמימוש בפועל של האחסון (SqliteAccountRepository). ההפרדה
 * הזו מאפשרת לבדוק קוד שמשתמש ב-repository (למשל LoginScreenMain
 * בעתיד) עם מימוש מזויף בטסטים, בלי להרים SQLite אמיתי בכל טסט.
 */
public interface AccountRepository {

    // יוצרת חשבון חדש עם elo התחלתי - זורקת UsernameTakenException אם
    // ה-username כבר קיים (ר' תיעוד שם למה זו checked exception).
    Account register(String username, String rawPassword) throws UsernameTakenException;

    // מנסה להתחבר עם username+password - Optional ריק אם ה-username לא
    // קיים *או* אם הסיסמה שגויה (בכוונה לא מבדילים בין שני המקרים כלפי
    // חוץ - זה מונע "user enumeration": מישהי לא אמורה להיות מסוגלת
    // לגלות אילו usernames קיימים לפי הבדל בהודעת השגיאה).
    Optional<Account> login(String username, String rawPassword);

    // ה-elo הנוכחי של username, או Optional.empty() אם אין חשבון כזה -
    // שלב 4 Part B (GameSession) קורא לזה כדי לחשב את הדירוג החדש לפני updateElo.
    Optional<Integer> currentElo(String username);

    // מעדכן את ה-elo של username לערך חדש שכבר חושב (ר' EloCalculator) -
    // הפרדה מכוונת בין "מה הדירוג החדש" (EloCalculator, טהור) ל-"לשמור
    // אותו" (כאן) - כדי ש-EloCalculator יהיה נבדק בלי SQLite בכלל.
    void updateElo(String username, int newElo);
}
