package kfchess.client;

import kfchess.engine.snapshot.CaptureEffect;
import kfchess.model.Position;
import kfchess.protocol.JumpDto;
import kfchess.protocol.PieceDto;
import kfchess.realtime.Motion;

import java.util.List;
import java.util.Map;


/** Gson deserialization target for a SNAPSHOT message; null collections are exposed as empty, not null. */
public class IncomingSnapshot {

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
    private boolean waitingForOpponent;

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


    public Integer disconnectSecondsRemaining() {
        return disconnectSecondsRemaining;
    }

    public boolean waitingForOpponent() {
        return waitingForOpponent;
    }
}
