package kfchess.server;

/**
 * נשלח לחיבור היחיד שממתין ל-Play כשעברה דקה בלי שנמצא/ה יריב/ה עם ELO
 * תואם (±100) - תיקון "Play" לפי המפרט המדויק: "waits for 1 min, if
 * can't find - pops up a message that can't find" (ר' GameServer.checkMatchmakingTimeout,
 * PROGRESS.md). מבנה זהה בכוונה ל-ErrorMessage (type קבוע + message חופשי) -
 * זה לא שגיאה במובן הטכני (שום דבר לא נכשל), אבל אותו דפוס DTO פשוט מתאים.
 */
public class MatchmakingTimeoutMessage {

    private final String type = "MATCHMAKING_TIMEOUT";
    private final String message;

    public MatchmakingTimeoutMessage(String message) {
        this.message = message;
    }
}
