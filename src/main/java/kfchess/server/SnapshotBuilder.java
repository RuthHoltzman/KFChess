package kfchess.server;

import kfchess.engine.GameCommandController;
import kfchess.engine.GameEngine;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Board;
import kfchess.model.ClientRole;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.protocol.JumpDto;
import kfchess.protocol.PieceDto;
import kfchess.protocol.SnapshotMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns game state (Board + GameEngine) into a {@link SnapshotMessage} - a pure domain-to-DTO
 * translation layer, extracted from {@link GameSession}. It keeps no state between calls.
 * <p>
 * The session-dependent details (restart vote, disconnect countdown, waiting for an opponent) are
 * <b>not</b> computed here; they arrive ready as parameters, because they depend on connection state
 * rather than on the board.
 */
public class SnapshotBuilder {

    /** One board scan: the pieces to broadcast, plus each piece's position - needed to locate active jumps. */
    private record BoardScan(List<PieceDto> pieces, Map<Piece, Position> positionByPiece) {
    }

    /** Assembles the full snapshot message for one viewer. */
    public SnapshotMessage build(Board board, GameEngine engine, GameCommandController commandController,
                                  ClientRole viewerRole, boolean restartRequestedByViewer,
                                  Integer disconnectSecondsRemaining, boolean waitingForOpponent) {
        BoardScan scan = scanBoard(board);
        Optional<Position> selected = viewerRole.toPieceColor()
                .flatMap(commandController::selectedPositionFor);
        List<Position> legalMoves = selected.map(engine::legalMovesFrom).orElse(List.of());
        String winner = engine.winner().map(PieceColor::name).orElse(null);

        return new SnapshotMessage(board.width(), board.height(), scan.pieces(), selected.orElse(null), legalMoves,
                scoresByName(engine), moveLogByName(engine), engine.isGameOver(), winner, engine.now(),
                engine.activeMotions(), collectJumps(engine, scan.positionByPiece()), engine.recentCaptureEffects(),
                restartRequestedByViewer, disconnectSecondsRemaining, waitingForOpponent);
    }

    /** Walks the board once, collecting both the DTOs to broadcast and a piece-to-position map. */
    private BoardScan scanBoard(Board board) {
        List<PieceDto> pieces = new ArrayList<>();
        Map<Piece, Position> positionByPiece = new HashMap<>();
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Position position = new Position(row, col);
                board.pieceAt(position).ifPresent(piece -> {
                    pieces.add(PieceDto.from(piece, position));
                    positionByPiece.put(piece, position);
                });
            }
        }
        return new BoardScan(pieces, positionByPiece);
    }

    /** Converts active jumps to DTOs; JumpVisual has no position of its own, so the scan map supplies it. */
    private List<JumpDto> collectJumps(GameEngine engine, Map<Piece, Position> positionByPiece) {
        List<JumpDto> jumps = new ArrayList<>();
        for (JumpVisual jump : engine.activeJumps()) {
            Position position = positionByPiece.get(jump.piece());
            if (position != null) {
                jumps.add(JumpDto.from(jump, position));
            }
        }
        return jumps;
    }

    /** Re-keys the score map by color name, for JSON. */
    private Map<String, Integer> scoresByName(GameEngine engine) {
        Map<String, Integer> byName = new HashMap<>();
        engine.scores().forEach((color, score) -> byName.put(color.name(), score));
        return byName;
    }

    /** Re-keys the move log by color name, for JSON. */
    private Map<String, List<String>> moveLogByName(GameEngine engine) {
        Map<String, List<String>> byName = new HashMap<>();
        engine.moveLog().forEach((color, log) -> byName.put(color.name(), log));
        return byName;
    }
}
