package kfchess.net;

/**
 * סוגי ההודעות שהלקוח יכול לשלוח לשרת. שדה ה-JSON "type" ({@link ClientCommand})
 * ממופה ישירות לשם קבוע כאן (Gson תואם שם-קבוע לפי הערך המחרוזתי) - ר'
 * ההחלטה ב-PROGRESS.md: קליק/קפיצה נשלחים כ-{"type":"CLICK","row":..,"col":..}.
 */
public enum ClientCommandType {
    CLICK,
    JUMP
}
