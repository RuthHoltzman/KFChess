package kfchess.account;

/**
 * חשבון שחקן/ית אחרי login/register מוצלח - רק המידע ש"מותר" לו לצאת
 * מ-AccountRepository החוצה (username + elo). בכוונה **בלי** password
 * hash בכלל: כל השדה הזה אמור להישאר כלוא בתוך שכבת ה-repository, כדי
 * שלא תהיה אפשרות "בטעות" להעביר/להדפיס/לשמור hash במקום שלא צריך.
 */
public record Account(String username, int elo) {
}
