package kfchess.client;

import kfchess.engine.snapshot.CaptureEffect;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceState;
import kfchess.model.Position;
import kfchess.protocol.JumpDto;
import kfchess.protocol.PieceDto;
import kfchess.realtime.Motion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Turns a decoded IncomingSnapshot back into real domain objects, preserving piece identity between messages by id. */
public class ClientSnapshotReconstructor {

    // Pieces seen in previous snapshots, keyed by Piece.id(), so identity is preserved across messages.
    private final Map<Long, Piece> knownPieces = new HashMap<>();

    /** The reconstructed state - exactly what SnapshotFactory.createSnapshot(...) needs as input. */
    public record Reconstructed(
            Board board,
            List<Motion> motions,
            List<JumpVisual> jumps,
            List<CaptureEffect> captureEffects,
            Position selected,
            List<Position> legalMoves,
            boolean gameOver,
            String winner,
            long now,
            Map<PieceColor, Integer> scores,
            Map<PieceColor, List<String>> moveLog,
            boolean restartRequestedByViewer,
            Integer disconnectSecondsRemaining,
            boolean waitingForOpponent
    ) {}

    /** Rebuilds a real Board plus Motion/JumpVisual lists from the decoded JSON, resolving piece identity throughout. */
    public Reconstructed reconstruct(IncomingSnapshot incoming) {
        Board board = Board.createDefault(incoming.boardHeightCells(), incoming.boardWidthCells());

        // Step 1: place every piece on the board, resolving its identity/state.
        for (PieceDto dto : incoming.pieces()) {
            Piece resolved = resolvePiece(dto.piece());
            board.placePiece(dto.position(), resolved);
        }

        // Step 2: rebuild motions/jumps using the same resolved piece objects.
        List<Motion> motions = new ArrayList<>();
        for (Motion motion : incoming.motions()) {
            Piece resolved = resolvePiece(motion.piece());
            motions.add(new Motion(resolved, motion.from(), motion.to(), motion.startTime(), motion.arrivalTime()));
        }

        List<JumpVisual> jumps = new ArrayList<>();
        for (JumpDto dto : incoming.jumps()) {
            Piece resolved = resolvePiece(dto.jump().piece());
            jumps.add(new JumpVisual(resolved, dto.jump().startTime(), dto.jump().endTime()));
        }

        return new Reconstructed(
                board, motions, jumps, incoming.captureEffects(),
                incoming.selected(), incoming.legalMoves(),
                incoming.gameOver(), incoming.winner(), incoming.now(),
                scoresByColor(incoming.scores()), moveLogByColor(incoming.moveLog()),
                incoming.restartRequestedByViewer(), incoming.disconnectSecondsRemaining(),
                incoming.waitingForOpponent());
    }


    /** Looks up by id: known piece -> sync its state and return the same object; unknown -> register and return as-is. */
    private Piece resolvePiece(Piece incoming) {
        Piece known = knownPieces.get(incoming.id());
        if (known == null) {
            knownPieces.put(incoming.id(), incoming);
            return incoming;
        }
        syncState(known, incoming.state());
        return known;
    }

    /** Moves an existing Piece to the desired state only through its own public methods (no direct field writes). */
    private void syncState(Piece piece, PieceState desired) {
        if (piece.state() == desired) {
            return;
        }
        switch (desired) {
            case IN_TRANSIT -> piece.markInTransit();
            case JUMPING -> piece.markJumping();
            case IDLE -> {
                if (piece.isInTransit()) {
                    piece.markArrived();
                } else if (piece.isJumping()) {
                    piece.markJumpEnded();
                }
            }
        }
    }

    /** Converts the wire format's Map<String,...> (JSON-friendly) back to Map<PieceColor,...>. */
    private Map<PieceColor, Integer> scoresByColor(Map<String, Integer> byName) {
        Map<PieceColor, Integer> byColor = new HashMap<>();
        byName.forEach((name, score) -> byColor.put(PieceColor.valueOf(name), score));
        return byColor;
    }

    /** Same conversion as {@link #scoresByColor}, for the per-color move log. */
    private Map<PieceColor, List<String>> moveLogByColor(Map<String, List<String>> byName) {
        Map<PieceColor, List<String>> byColor = new HashMap<>();
        byName.forEach((name, log) -> byColor.put(PieceColor.valueOf(name), log));
        return byColor;
    }
}
