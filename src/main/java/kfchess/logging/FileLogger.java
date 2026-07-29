package kfchess.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Shared text logger for the server and the client: an operational log (who connected, errors,
 * how a game was created) - unrelated to the chess move log shown in the side panel.
 * <p>
 * A new file per run, not one growing file: it makes it easy to point at the specific run where
 * something happened, and several client processes running at once never share a file.
 * <p>
 * log() is synchronized because within one process several threads can write at the same time
 * (the network thread and the tick thread), and lines would otherwise interleave.
 */
public class FileLogger {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LINE_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Path LOGS_DIRECTORY = Path.of("logs");

    private final PrintWriter writer;

    /** prefix is "server" or "client"; it only sets the start of the file name (logs/server_&lt;timestamp&gt;.log). */
    public FileLogger(String prefix) {
        this.writer = openWriter(prefix);
    }

    /**
     * Opens the log file, or returns null if it can't be created (e.g. a permissions problem).
     * Logging is a convenience, not core behavior, so a failure here warns once and never brings
     * the server or client down - log() checks for null and quietly does nothing.
     */
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

    /** Writes one timestamped line; a no-op if the log file couldn't be opened. */
    public synchronized void log(String message) {
        if (writer == null) {
            return;
        }
        writer.println("[" + LocalDateTime.now().format(LINE_TIMESTAMP) + "] " + message);
    }

    /**
     * For an orderly shutdown. Not currently called by PlayServer or PlayClient - autoFlush already
     * writes each line to disk immediately, so nothing is lost without an explicit close.
     */
    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
