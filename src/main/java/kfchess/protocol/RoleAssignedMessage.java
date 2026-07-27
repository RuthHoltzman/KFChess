package kfchess.protocol;

/**
 * נשלח פעם אחת לכל חיבור, מיד אחרי onOpen: מודיע ללקוח באיזה תפקיד הוא
 * משחק במשחק הזה - הראשון שמתחבר ל-GameSession נתון מקבל WHITE, השני
 * BLACK, כל השאר SPECTATOR (ר' GameSession.assignRole).
 * <p>
 * DTO ליציאה בלבד (משמש רק ל-gson.toJson) - אין getters, ואין בדיקת
 * null על הפרמטרים כי הקריאה היחידה אליו (מ-GameServer) כבר מבטיחה ערכים תקינים.
 */
public class RoleAssignedMessage {

    private final String type = "ROLE_ASSIGNED";
    private final String role;
    private final String gameId;

    public RoleAssignedMessage(String role, String gameId) {
        this.role = role;
        this.gameId = gameId;
    }
}
