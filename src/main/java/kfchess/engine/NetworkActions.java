package kfchess.engine;

import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * מנתב קלט מודע-צבע עבור GameEngine, לשימוש השרת: מאפשר לשני שחקנים
 * לפעול בו-זמנית ובאופן עצמאי על אותו GameEngine - כל צבע עם "בחירה
 * נוכחית" משלו, בלי שקליק של שחקן אחד יתפרש כהשלמת מהלך של השני
 * (מה שהיה קורה עם selectedPosition היחיד/משותף של GameEngine, שמתאים
 * למשחק מקומי חד-שחקן אבל לא לרשת).
 * <p>
 * חי באותה חבילה (kfchess.engine) כדי לראות את החברים package-private
 * של GameEngine (isAvailableToAct/tryMove/beginJump/advanceGameState) -
 * כל לוגיקת חוקי המשחק עצמה נשארת אך ורק ב-GameEngine; המחלקה הזו רק
 * מנתבת קליק לשחקן הנכון ואוכפת שכל שחקן נוגע רק בכלים של עצמו.
 */
public class NetworkActions {

    private final GameEngine engine;
    private final Map<PieceColor, Position> selectedPositionByColor = new EnumMap<>(PieceColor.class);

    public NetworkActions(GameEngine engine) {
        this.engine = engine;
    }

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

    /** מה השחקן בצבע הזה בחר כרגע (אם בכלל) - לשימוש השרת בבניית snapshot. */
    public Optional<Position> selectedPositionFor(PieceColor color) {
        return Optional.ofNullable(selectedPositionByColor.get(color));
    }

    private void trySelect(PieceColor actingColor, Position clicked) {
        engine.board().pieceAt(clicked).ifPresent(piece -> {
            if (piece.color() == actingColor && engine.isAvailableToAct(piece)) {
                selectedPositionByColor.put(actingColor, clicked);
            }
        });
    }

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
