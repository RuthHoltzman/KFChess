package kfchess.net;

import java.util.List;
import java.util.Map;

/**
 * מצב הלוח המלא, נשלח לכל המחוברים בכל טיק (ר' GameSession.broadcastSnapshot).
 * "selected" ו-"legalMoves" הם ביחס לצבע של *הנמען הספציפי* - כל חיבור
 * מקבל אובייקט שונה, בהתאם לצבע ששויך לו (לבן/שחור רואים רק את הבחירה
 * שלהם; צופה מקבל selected=null, legalMoves=[]).
 */
public class SnapshotMessage {

    private final String type = "SNAPSHOT";
    private final List<PieceDto> pieces;
    private final PositionDto selected;
    private final List<PositionDto> legalMoves;
    private final Map<String, Integer> scores;
    private final Map<String, List<String>> moveLog;
    private final boolean gameOver;
    private final String winner;
    private final long now;

    public SnapshotMessage(List<PieceDto> pieces, PositionDto selected, List<PositionDto> legalMoves,
                           Map<String, Integer> scores, Map<String, List<String>> moveLog,
                           boolean gameOver, String winner, long now) {
        this.pieces = pieces;
        this.selected = selected;
        this.legalMoves = legalMoves;
        this.scores = scores;
        this.moveLog = moveLog;
        this.gameOver = gameOver;
        this.winner = winner;
        this.now = now;
    }
}
