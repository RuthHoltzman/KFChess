package kfchess.net.client;

import kfchess.engine.snapshot.CaptureEffect;
import kfchess.model.Position;
import kfchess.net.JumpDto;
import kfchess.net.PieceDto;
import kfchess.realtime.Motion;

import java.util.List;
import java.util.Map;

/**
 * "תמונת מראה" של SnapshotMessage, בצד הלקוח - אותם שדות/שמות בדיוק,
 * כדי ש-Gson יוכל לפענח את ה-JSON שה-SNAPSHOT מגיע איתו ישירות לתוך
 * מחלקה טיפוסית (בלי לגעת ב-SnapshotMessage עצמו, שהוא ייעודי לכיוון
 * שרת->JSON ואין לו getters). זו לא עוד "שכבה" - היא רק ה-DTO ההפוך
 * (JSON->טיפוסים), באותה רוח שבה ClientCommand כבר משמש הפוך בשני
 * הכיוונים (ר' תיעוד ClientCommand.java).
 * <p>
 * הפענוח כאן עדיין "גולמי": pieces/jumps מכילים PieceDto/JumpDto
 * שה-piece שבתוכם הוא אובייקט חדש בכל הודעה (Gson לא שומר זהות בין
 * הודעות) - זה בדיוק מה ש-ClientSnapshotReconstructor פותר בשלב הבא.
 */
public class IncomingSnapshot {

    private String type;
    private int boardWidthCells;
    private int boardHeightCells;
    private List<PieceDto> pieces;
    private Position selected;
    private List<Position> legalMoves;
    private Map<String, Integer> scores;
    private Map<String, List<String>> moveLog;
    private boolean gameOver;
    private String winner;
    private long now;
    private List<Motion> motions;
    private List<JumpDto> jumps;
    private List<CaptureEffect> captureEffects;
    private boolean restartRequestedByViewer;
    private Integer disconnectSecondsRemaining;

    public String type() {
        return type;
    }

    public int boardWidthCells() {
        return boardWidthCells;
    }

    public int boardHeightCells() {
        return boardHeightCells;
    }

    public List<PieceDto> pieces() {
        return pieces == null ? List.of() : pieces;
    }

    public Position selected() {
        return selected;
    }

    public List<Position> legalMoves() {
        return legalMoves == null ? List.of() : legalMoves;
    }

    public Map<String, Integer> scores() {
        return scores == null ? Map.of() : scores;
    }

    public Map<String, List<String>> moveLog() {
        return moveLog == null ? Map.of() : moveLog;
    }

    public boolean gameOver() {
        return gameOver;
    }

    public String winner() {
        return winner;
    }

    public long now() {
        return now;
    }

    public List<Motion> motions() {
        return motions == null ? List.of() : motions;
    }

    public List<JumpDto> jumps() {
        return jumps == null ? List.of() : jumps;
    }

    public List<CaptureEffect> captureEffects() {
        return captureEffects == null ? List.of() : captureEffects;
    }

    public boolean restartRequestedByViewer() {
        return restartRequestedByViewer;
    }

    // null (לא 0) כשאין אף אחד ב"חלון חסד" כרגע - Gson משאיר את השדה
    // null אם ה-JSON לא כלל אותו (בדיוק כמו כל שדה חסר אחר), אז אין
    // צורך בטיפול מיוחד כאן, בניגוד ל-List/Map (שם יש ברירת מחדל ל-List.of()/Map.of()).
    public Integer disconnectSecondsRemaining() {
        return disconnectSecondsRemaining;
    }
}
