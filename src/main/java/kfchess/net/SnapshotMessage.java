package kfchess.net;

import kfchess.engine.snapshot.CaptureEffect;
import kfchess.model.Position;
import kfchess.realtime.Motion;

import java.util.List;
import java.util.Map;

/**
 * מצב הלוח המלא, נשלח לכל המחוברים בכל טיק (ר' GameSession.broadcastSnapshot).
 * "selected" ו-"legalMoves" הם ביחס לצבע של *הנמען הספציפי* - כל חיבור
 * מקבל אובייקט שונה, בהתאם לצבע ששויך לו (לבן/שחור רואים רק את הבחירה
 * שלהם; צופה מקבל selected=null, legalMoves=[]).
 * <p>
 * "motions"/"jumps"/"captureEffects" הם אותו מידע בדיוק שה-UI המקומי
 * (Swing, ר' SnapshotFactory) כבר משתמש בו כדי לצייר אנימציות - תנועה
 * חלקה בין משבצות, קשת קפיצה, והבהוב לכידה. הלקוח מחשב התקדמות (0..1)
 * מתוך "now" (שמגיע כאן באותה הודעה) מול startTime/arrivalTime של כל
 * פריט - בלי צורך בסנכרון שעונים בין השרת ללקוח.
 * <p>
 * Position/Motion/CaptureEffect משודרים כאן *ישירות* (בלי DTO עוטף) -
 * הם כבר בדיוק בצורה שרוצים לשלוח, ואין עדיין אף לקוח שתלוי בצורה
 * "יציבה" שדורשת שכבת בידוד; רק piece/jump מקבלים עטיפה (PieceDto/
 * JumpDto) כי בכוונה אין להם מיקום משלהם (ר' Piece.java).
 */
public class SnapshotMessage {

    private final String type = "SNAPSHOT";
    private final int boardWidthCells;
    private final int boardHeightCells;
    private final List<PieceDto> pieces;
    private final Position selected;
    private final List<Position> legalMoves;
    private final Map<String, Integer> scores;
    private final Map<String, List<String>> moveLog;
    private final boolean gameOver;
    private final String winner;
    private final long now;
    private final List<Motion> motions;
    private final List<JumpDto> jumps;
    private final List<CaptureEffect> captureEffects;

    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects) {
        this.boardWidthCells = boardWidthCells;
        this.boardHeightCells = boardHeightCells;
        this.pieces = pieces;
        this.selected = selected;
        this.legalMoves = legalMoves;
        this.scores = scores;
        this.moveLog = moveLog;
        this.gameOver = gameOver;
        this.winner = winner;
        this.now = now;
        this.motions = motions;
        this.jumps = jumps;
        this.captureEffects = captureEffects;
    }
}
