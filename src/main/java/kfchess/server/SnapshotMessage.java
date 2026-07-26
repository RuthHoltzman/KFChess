package kfchess.server;

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
    // רלוונטי רק כש-gameOver=true: "הנמען הספציפי הזה כבר ביקש/ה RESTART,
    // מחכה שהצד השני גם יבקש" - שני הצדדים צריכים לבקש RESTART כדי
    // שהלוח יתאפס בפועל (ר' GameSession.applyRestartVote), אחרת מישהו
    // יכול "לברוח" מהפסד לבד. תמיד false לצופה (SPECTATOR).
    private final boolean restartRequestedByViewer;
    // שניות שנותרו עד שהצד שהתנתק (אם יש כזה) יפסיד אוטומטית - ר'
    // GameSession.disconnectSecondsRemaining/DISCONNECT_GRACE_MILLIS. null
    // (לא 0) כשאין אף אחד ב"חלון חסד" כרגע - כדי שהלקוח יוכל להבדיל בין
    // "0 שניות נשארו" (רגע לפני שהיריב מוכרז כמנצח) לבין "אין בכלל ניתוק
    // פעיל" בלי לבדוק gameOver בנוסף (בניגוד ל-restartRequestedByViewer,
    // זה יכול להיות true גם כשהמשחק *לא* נגמר - זו בדיוק הנקודה).
    private final Integer disconnectSecondsRemaining;

    // חתימה ישנה, בלי restartRequestedByViewer/disconnectSecondsRemaining -
    // נשארת כדי ש-ClientSnapshotReconstructorTest/MessageDtoTest הקיימים
    // ימשיכו לעבוד בלי שינוי; שקולה ל-restartRequestedByViewer=false,
    // disconnectSecondsRemaining=null (המקרה הרגיל - אין restart וגם אין ניתוק).
    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects) {
        this(boardWidthCells, boardHeightCells, pieces, selected, legalMoves, scores, moveLog, gameOver, winner, now,
                motions, jumps, captureEffects, false, null);
    }

    // חתימה ביניים, עם restartRequestedByViewer אבל בלי disconnectSecondsRemaining -
    // נשארת כדי שקוד/טסטים שנכתבו בסבב ה-Restart (לפני שלב 5) ימשיכו
    // לעבוד בלי שינוי; שקולה ל-disconnectSecondsRemaining=null.
    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects,
                           boolean restartRequestedByViewer) {
        this(boardWidthCells, boardHeightCells, pieces, selected, legalMoves, scores, moveLog, gameOver, winner, now,
                motions, jumps, captureEffects, restartRequestedByViewer, null);
    }

    public SnapshotMessage(int boardWidthCells, int boardHeightCells, List<PieceDto> pieces, Position selected,
                           List<Position> legalMoves, Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now,
                           List<Motion> motions, List<JumpDto> jumps, List<CaptureEffect> captureEffects,
                           boolean restartRequestedByViewer, Integer disconnectSecondsRemaining) {
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
        this.restartRequestedByViewer = restartRequestedByViewer;
        this.disconnectSecondsRemaining = disconnectSecondsRemaining;
    }
}
