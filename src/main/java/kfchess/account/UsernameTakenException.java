package kfchess.account;

/**
 * נזרקת מ-AccountRepository.register כשמנסים ליצור חשבון עם username
 * שכבר קיים. checked בכוונה (לא RuntimeException) - זו לא שגיאת תכנות
 * אלא תרחיש עסקי צפוי לגמרי (מישהי בחרה שם שכבר תפוס), בדיוק כמו
 * שחלון ה-Register אמור להציג הודעה ולתת לנסות שם אחר, לא לקרוס.
 */
public class UsernameTakenException extends Exception {

    public UsernameTakenException(String username) {
        super("username already taken: " + username);
    }
}
