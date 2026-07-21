package kfchess.net;

/**
 * DTO גולמי להודעה שמגיעה מהלקוח, לפני שהיא מנותבת ל-NetworkActions.
 * נבנה ע"י Gson מ-JSON גולמי (ר' GameSession.enqueueCommand) - אין בנאי
 * מפורש בכוונה, Gson ממלא את השדות ישירות דרך reflection.
 * <p>
 * מיקום (row/col) הוא מיקום על הלוח, לא פיקסלים - הלקוח כבר עושה
 * pixel→Position בעצמו לפני השליחה (ר' BoardMapper בצד הלקוח).
 */
public class ClientCommand {

    private ClientCommandType type;
    private Integer row;
    private Integer col;

    public ClientCommandType type() {
        return type;
    }

    public Integer row() {
        return row;
    }

    public Integer col() {
        return col;
    }

    /** תקינות בסיסית - שדות חובה קיימים, לפני שממירים ל-Position. */
    public boolean isValid() {
        return type != null && row != null && col != null;
    }
}
