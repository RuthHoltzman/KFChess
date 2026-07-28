package kfchess.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את FileLogger - שלב 6 חלק 2 (לוגים טכניים/תפעוליים, לא קשור
 * ל-moveLog). קבצי הבדיקה נכתבים בפועל ל-logs/ (כמו בייצור) עם קידומת
 * ייחודית לכל טסט, כדי לא להתנגש בין הטסטים - *.log כבר ב-.gitignore,
 * אז הם לא נכנסים לגיט בטעות (וגם logs/ לא נשמרת ריקה - git לא עוקב
 * אחרי תיקיות ריקות ממילא).
 */
class FileLoggerTest {

    @Test
    void log_writesTimestampedLineToFileUnderLogsDirectory() throws IOException {
        FileLogger logger = new FileLogger("test-single-line");
        logger.log("hello world");
        logger.close();

        Path created = findCreatedFile("test-single-line_");
        List<String> lines = Files.readAllLines(created);

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).endsWith("hello world"));
        assertTrue(lines.get(0).startsWith("[")); // חותמת זמן בתחילת השורה
    }

    @Test
    void log_multipleCalls_appendsEachAsSeparateLine() throws IOException {
        FileLogger logger = new FileLogger("test-multi-line");
        logger.log("first");
        logger.log("second");
        logger.close();

        Path created = findCreatedFile("test-multi-line_");
        List<String> lines = Files.readAllLines(created);

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).endsWith("first"));
        assertTrue(lines.get(1).endsWith("second"));
    }

    // מוצאת את קובץ הלוג שנוצר עבור הקידומת הזו (יש בו גם חותמת תאריך/שעה
    // בשם, ר' FileLogger - לכן לא ניתן לחזות את השם המדויק מראש).
    private Path findCreatedFile(String prefix) throws IOException {
        try (Stream<Path> files = Files.list(Path.of("logs"))) {
            return files.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no log file found with prefix " + prefix));
        }
    }
}
