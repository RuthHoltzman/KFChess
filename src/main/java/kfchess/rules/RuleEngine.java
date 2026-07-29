package kfchess.rules;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceKind;
import kfchess.model.Position;


/** Checks whether a move is legal: in bounds, not onto your own piece, and matches the piece's movement rule. */
public class RuleEngine {

    private final PieceRules pieceRules;

    public RuleEngine(PieceRules pieceRules) {
        this.pieceRules = pieceRules;
    }

    public RuleEngine() {
        this(new PieceRules());
    }

    /** Whether moving the given piece from "from" to "to" is legal right now. */
    public boolean isLegalMove(Board board, Piece piece, Position from, Position to) {
        if (!board.isWithinBounds(to)) {
            return false;
        }
        if (board.isOccupiedBySameColor(to, piece.color())) {
            return false;
        }
        return pieceRules.ruleFor(piece.kind()).isLegal(board, from, to);
    }


    /** Overrides the movement rule for a piece kind (extension point - currently exercised only by tests). */
    public void registerCustomRule(PieceKind kind, PieceRules.MoveRule rule) {
        pieceRules.register(kind, rule);
    }


    /** Whether every square strictly between "from" and "to" (along a straight/diagonal line) is empty. */
    static boolean isPathClear(Board board, Position from, Position to) {
        int stepRow = Integer.compare(to.row(), from.row());
        int stepCol = Integer.compare(to.col(), from.col());

        Position current = from.offset(stepRow, stepCol);
        while (!current.equals(to)) {
            if (!board.isEmpty(current)) {
                return false;
            }
            current = current.offset(stepRow, stepCol);
        }
        return true;
    }
}
