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


/** Tracks per-color score and move-log notation, publishing bus events as they change. */
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


    /** Records a normal move (or capture), updating score and notation, and plays the matching sound. */
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


    /** Records an "air capture": the attacker vanishes and the jumping defender is credited with the score. */
    public void recordCounterCapture(Piece defender, Piece attacker, Position from, Position to) {
        String attackerNotation = attacker.kind().code() + squareName(from) + "x" + squareName(to) + "?!";
        moveLog.get(attacker.color()).add(attackerNotation);
        bus.publish(new MoveLoggedEvent(attacker.color(), attackerNotation));

        scores.merge(defender.color(), attacker.kind().value(), Integer::sum);
        bus.publish(new ScoreUpdatedEvent(defender.color(), scores.get(defender.color())));

        String defenderNotation = defender.kind().code() + squareName(to) + "x" + squareName(from) + " (jump)";
        moveLog.get(defender.color()).add(defenderNotation);
        bus.publish(new MoveLoggedEvent(defender.color(), defenderNotation));

        bus.publish(new SoundEvent(SoundEvent.Type.CAPTURE));
    }

    /** Records a chained slide that got blocked by a same-color piece partway through. */
    public void recordBlockedMove(Piece movingPiece, Position from, Position blockedAt) {
        String notation = movingPiece.kind().code() + squareName(from) + "-" + squareName(blockedAt) + " (blocked)";
        moveLog.get(movingPiece.color()).add(notation);
        bus.publish(new MoveLoggedEvent(movingPiece.color(), notation));
    }

    /** Formats a board position as standard algebraic notation (e.g. "e4"). */
    private String squareName(Position pos) {
        char file = (char) ('a' + pos.col());
        int rank = board.height() - pos.row();
        return "" + file + rank;
    }

    public Map<PieceColor, Integer> scores() {
        return Map.copyOf(scores);
    }

    public Map<PieceColor, List<String>> moveLog() {
        Map<PieceColor, List<String>> copy = new EnumMap<>(PieceColor.class);
        for (Map.Entry<PieceColor, List<String>> entry : moveLog.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }
}
