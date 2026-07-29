package kfchess.engine;

import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** The server-side controller: routes a per-color CLICK/JUMP to PlayEngine, one independent selection per color. */
public class PlayCommandController {

    private final PlayEngine engine;
    private final Map<PieceColor, Position> selectedPositionByColor = new EnumMap<>(PieceColor.class);

    public PlayCommandController(PlayEngine engine) {
        this.engine = engine;
    }

    /** First click for a color selects a piece; second click acts on that selection. */
    public void handleClick(PieceColor actingColor, Position clicked) {
        engine.advanceGameState();
        if (engine.isGameOver() || !engine.board().isWithinBounds(clicked)) {
            return;
        }
        Position currentSelection = selectedPositionByColor.get(actingColor);
        if (currentSelection == null) {
            trySelect(actingColor, clicked);
        } else {
            tryActOnSelection(actingColor, currentSelection, clicked);
        }
    }

    /** Starts a jump for the piece at the target, if it belongs to the acting color and is available. */
    public void handleJump(PieceColor actingColor, Position target) {
        engine.advanceGameState();
        if (engine.isGameOver()) {
            return;
        }
        engine.board().pieceAt(target).ifPresent(piece -> {
            if (piece.color() == actingColor && engine.isAvailableToAct(piece)) {
                engine.beginJump(piece);
                if (target.equals(selectedPositionByColor.get(actingColor))) {
                    selectedPositionByColor.remove(actingColor);
                }
            }
        });
    }

    /** What this color currently has selected, if anything - used when building a snapshot for that viewer. */
    public Optional<Position> selectedPositionFor(PieceColor color) {
        return Optional.ofNullable(selectedPositionByColor.get(color));
    }

    /** Selects the clicked piece, if it belongs to the acting color and is available. */
    private void trySelect(PieceColor actingColor, Position clicked) {
        engine.board().pieceAt(clicked).ifPresent(piece -> {
            if (piece.color() == actingColor && engine.isAvailableToAct(piece)) {
                selectedPositionByColor.put(actingColor, clicked);
            }
        });
    }

    /** Re-selects another own piece, or attempts the move to the clicked square. */
    private void tryActOnSelection(PieceColor actingColor, Position selected, Position clicked) {
        Optional<Piece> selectedPiece = engine.board().pieceAt(selected);
        if (selectedPiece.isEmpty()) {
            selectedPositionByColor.remove(actingColor);
            return;
        }

        boolean clickedOwnAvailablePiece = engine.board().pieceAt(clicked)
                .map(p -> p.isSameColor(selectedPiece.get()) && engine.isAvailableToAct(p))
                .orElse(false);

        if (clickedOwnAvailablePiece) {
            selectedPositionByColor.put(actingColor, clicked);
            return;
        }

        engine.tryMove(selectedPiece.get(), selected, clicked);
        selectedPositionByColor.remove(actingColor);
    }
}
