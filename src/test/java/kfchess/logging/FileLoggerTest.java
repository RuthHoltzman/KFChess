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
 * Covers FileLogger (the operational log, unrelated to the chess move log).
 * These tests really do write into logs/, like production, each with its own prefix so they can't
 * collide. *.log is already gitignored, so nothing lands in git by accident.
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
        assertTrue(lines.get(0).startsWith("[")); // timestamp at the start of the line
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

    // Finds the log file created for this prefix. The name also contains a timestamp,
    // so the exact file name can't be predicted in advance.
    private Path findCreatedFile(String prefix) throws IOException {
        try (Stream<Path> files = Files.list(Path.of("logs"))) {
            return files.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no log file found with prefix " + prefix));
        }
    }
}
