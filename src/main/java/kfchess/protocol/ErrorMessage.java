package kfchess.protocol;

/**
 * נשלח כשהלקוח שולח הודעה פגומה (JSON לא תקין, שדות חסרים) או פקודה
 * שלא ניתן לבצע - כדי שהשרת יגיב בצורה מסודרת במקום לקרוס/להתעלם בשקט.
 */
public class ErrorMessage {

    private final String type = "ERROR";
    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }
}
