package kfchess.engine;

import kfchess.bus.EventBus;
import kfchess.bus.MoveLoggedEvent;
import kfchess.bus.ScoreUpdatedEvent;
import kfchess.bus.SoundEvent;
import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * "מה קרה" - לא "מה מותר": עוקב אחרי ניקוד ויומן מהלכים (בסימון
 * שח-מטי) לכל צבע, ומפרסם לבוס את האירועים המתאימים. הוצא מ-GameEngine
 * כי זה בעיקר ריפורטינג/הצגה, לא חוק משחק - GameEngine עדיין מחליט
 * *אם* מהלך קרה בכלל; המחלקה הזו רק רושמת אותו אחרי המעשה.
 */
public class MoveHistory {

    private final Board board;
    private final EventBus bus;
    private final Map<PieceColor, Integer> scores = new EnumMap<>(PieceColor.class);
    private final Map<PieceColor, List<String>> moveLog = new EnumMap<>(PieceColor.class);

    public MoveHistory(Board board, EventBus bus) {
        this.board = board;
        this.bus = bus;
        for (PieceColor color : PieceColor.values()) {
            scores.put(color, 0);
            moveLog.put(color, new ArrayList<>());
        }
    }

    /**
     * מעדכן ניקוד (אם הייתה לכידה) ומוסיף שורה לרשימת המהלכים של הצבע
     * שביצע את המהלך - נקרא אחרי שהמהלך כבר אושר ובוצע.
     */
    public void recordMove(Piece movingPiece, Position from, Position to, Piece captured) {
        boolean isCapture = captured != null;
        if (isCapture) {
            scores.merge(movingPiece.color(), captured.kind().value(), Integer::sum);
            bus.publish(new ScoreUpdatedEvent(movingPiece.color(), scores.get(movingPiece.color())));
        }
        String notation = movingPiece.kind().code() + squareName(from)
                + (isCapture ? "x" : "-") + squareName(to);
        moveLog.get(movingPiece.color()).add(notation);
        bus.publish(new MoveLoggedEvent(movingPiece.color(), notation));
        bus.publish(new SoundEvent(isCapture ? SoundEvent.Type.CAPTURE : SoundEvent.Type.MOVE));
    }

    /** מהלך תקיפה שנכשל מול כלי קופץ - מתועד ברשימת המהלכים בלי שינוי ניקוד. */
    public void recordFailedCapture(Piece movingPiece, Position from, Position to) {
        String notation = movingPiece.kind().code() + squareName(from) + "x" + squareName(to) + "?!";
        moveLog.get(movingPiece.color()).add(notation);
        bus.publish(new MoveLoggedEvent(movingPiece.color(), notation));
        bus.publish(new SoundEvent(SoundEvent.Type.ILLEGAL));
    }

    /** מהלך שנעצר כי כלי ידידותי היה במשבצת הבאה - "כמעט התנגשות". */
    public void recordBlockedMove(Piece movingPiece, Position from, Position blockedAt) {
        String notation = movingPiece.kind().code() + squareName(from) + "-" + squareName(blockedAt) + " (blocked)";
        moveLog.get(movingPiece.color()).add(notation);
        bus.publish(new MoveLoggedEvent(movingPiece.color(), notation));
    }

    /** ממיר Position לסימון שח-מטי מוכר (עמודה a.. + שורה ממוספרת מלמטה). */
    private String squareName(Position pos) {
        char file = (char) ('a' + pos.col());
        int rank = board.height() - pos.row();
        return "" + file + rank;
    }

    /** עותק הגנתי: ניקוד נוכחי לכל צבע (למשל להצגה בפאנל הצד). */
    public Map<PieceColor, Integer> scores() {
        return Map.copyOf(scores);
    }

    /** עותק הגנתי: רשימת המהלכים (בסימון שח-מטי) שביצע כל צבע עד כה. */
    public Map<PieceColor, List<String>> moveLog() {
        Map<PieceColor, List<String>> copy = new EnumMap<>(PieceColor.class);
        for (Map.Entry<PieceColor, List<String>> entry : moveLog.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }
}
