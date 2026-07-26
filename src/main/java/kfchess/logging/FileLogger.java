package kfchess.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * לוגר טקסט משותף לשרת וללקוח (שלב 6, חלק 2 - "Store logs on both server
 * and client side, for all of the client/server activity", לפי המצגת
 * המקורית). זה לא קשור בכלל ל-moveLog (רישום מהלכי השחמט, שכבר קיים
 * מהשלבים המוקדמים ומוצג בפאנל הצדדי) - זה לוג טכני/תפעולי: מי התחבר/
 * התנתק, שגיאות, איך משחק נוצר וכו'.
 * <p>
 * קובץ חדש בכל הרצה (לא קובץ אחד שמצטבר) - רות בחרה את זה במפורש, כדי
 * שקל יהיה להצביע על "ההרצה הספציפית שבה קרה משהו", ובעיקר כדי שכמה
 * תהליכי לקוח שרצים בו-זמנית (כל חלון LoginScreenMain הוא JVM נפרד) לא
 * "יתחרו" על אותו קובץ - לכל הרצה יש שם קובץ ייחודי (קידומת + חותמת
 * זמן), אז אין בכלל מצב של כתיבה בו-זמנית מכמה תהליכים לאותו קובץ.
 * <p>
 * synchronized על log(): בתוך תהליך *אחד* (למשל GameServer) עדיין אפשר
 * שכמה threads יכתבו בו-זמנית (thread הרשת מול thread הטיק) - בלי נעילה
 * שורות היו עלולות "להתערבב" זו בזו בקובץ.
 */
public class FileLogger {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LINE_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Path LOGS_DIRECTORY = Path.of("logs");

    private final PrintWriter writer;

    // prefix - "server" או "client", קובע רק את תחילת שם הקובץ (logs/server_<timestamp>.log).
    public FileLogger(String prefix) {
        this.writer = openWriter(prefix);
    }

    // אם אי-אפשר ליצור/לפתוח את קובץ הלוג (לדוגמה בעיית הרשאות) - לא
    // מפילה את השרת/לקוח בגלל זה בכלל: מדפיסה אזהרה אחת ל-System.err
    // וממשיכה בלי כתיבה לקובץ (log() בודקת null ולא עושה כלום). הלוג
    // הוא כלי עזר, לא חלק קריטי מהתפקוד עצמו.
    private static PrintWriter openWriter(String prefix) {
        try {
            Files.createDirectories(LOGS_DIRECTORY);
            String fileName = prefix + "_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".log";
            return new PrintWriter(Files.newBufferedWriter(LOGS_DIRECTORY.resolve(fileName)), true); // autoFlush=true
        } catch (IOException failedToOpen) {
            System.err.println("FileLogger: could not open log file (" + failedToOpen.getMessage() + ") - continuing without file logging");
            return null;
        }
    }

    public synchronized void log(String message) {
        if (writer == null) {
            return;
        }
        writer.println("[" + LocalDateTime.now().format(LINE_TIMESTAMP) + "] " + message);
    }

    // נקראת רק בסגירה מסודרת (למשל אם בעתיד יתווסף shutdown hook) - כרגע
    // לא נקראת בפועל מ-GameServer/GameClient (autoFlush=true כבר מבטיח
    // שהשורות נכתבות לדיסק מיד, גם בלי close מפורש).
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
