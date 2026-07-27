package kfchess.protocol;

/**
 * DTO דו-כיווני להודעת קליק/קפיצה: בצד השרת נבנה ע"י Gson מ-JSON גולמי
 * (ר' GameSession.enqueueCommand, ללא צורך בבנאי - reflection ישיר על
 * השדות). בצד הלקוח (ר' GameClient) נבנה דרך הבנאי הציבורי ואז מומר
 * ל-JSON לפני שליחה - אותו DTO בשני הכיוונים, בלי לשכפל מבנה.
 * <p>
 * מיקום (row/col) הוא מיקום על הלוח, לא פיקסלים - הלקוח כבר עושה
 * pixel→Position בעצמו לפני השליחה (ר' BoardMapper בצד הלקוח).
 */
public class ClientCommand {

    private ClientCommandType type;
    private Integer row;
    private Integer col;

    /** לשימוש הלקוח בלבד - בונה פקודה לשליחה. השרת לא משתמש בבנאי הזה, רק ב-Gson.fromJson. */
    public ClientCommand(ClientCommandType type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
    }

    public ClientCommandType type() {
        return type;
    }

    public Integer row() {
        return row;
    }

    public Integer col() {
        return col;
    }

    /**
     * תקינות בסיסית - שדות חובה קיימים, לפני שממירים ל-Position. RESTART
     * לא צריך row/col בכלל (אין לו מיקום על הלוח) - הלקוח שולח 0,0
     * "דמה" שפשוט לא נקראים, אבל גם JSON גולמי בלי row/col בכלל (למשל
     * {"type":"RESTART"}) תקין.
     */
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        return type == ClientCommandType.RESTART || (row != null && col != null);
    }
}
