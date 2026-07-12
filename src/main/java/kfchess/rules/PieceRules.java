package kfchess.rules;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;

import java.util.EnumMap;
import java.util.Map;

public final class PieceRules {

    @FunctionalInterface
    public interface MoveRule {
        boolean isLegal(Board board, Position from, Position to);
    }

    private final Map<PieceKind, MoveRule> rules = new EnumMap<>(PieceKind.class);

    public PieceRules() {
        rules.put(PieceKind.KING, (board, from, to) -> 
            Math.abs(from.row() - to.row()) <= 1 && Math.abs(from.col() - to.col()) <= 1);
            
        rules.put(PieceKind.ROOK, (board, from, to) -> 
            (from.row() == to.row() || from.col() == to.col()) && RuleEngine.isPathClear(board, from, to));
            
        rules.put(PieceKind.BISHOP, (board, from, to) -> 
            Math.abs(from.row() - to.row()) == Math.abs(from.col() - to.col()) && RuleEngine.isPathClear(board, from, to));
            
        rules.put(PieceKind.QUEEN, (board, from, to) -> 
            (from.row() == to.row() || from.col() == to.col() || Math.abs(from.row() - to.row()) == Math.abs(from.col() - to.col())) 
            && RuleEngine.isPathClear(board, from, to));
            
        rules.put(PieceKind.KNIGHT, (board, from, to) -> {
            int dRow = Math.abs(from.row() - to.row());
            int dCol = Math.abs(from.col() - to.col());
            return (dRow == 2 && dCol == 1) || (dRow == 1 && dCol == 2);
        });
        
        rules.put(PieceKind.PAWN, PieceRules::pawnMove);
    }

    public MoveRule ruleFor(PieceKind kind) {
        return rules.getOrDefault(kind, (board, from, to) -> false);
    }

    public void register(PieceKind kind, MoveRule rule) {
        rules.put(kind, rule);
    }

    private static boolean pawnMove(Board board, Position from, Position to) {
        Piece pawn = board.pieceAt(from).orElseThrow();
        int direction = (pawn.color() == PieceColor.WHITE) ? -1 : 1;
        int startRow = (pawn.color() == PieceColor.WHITE) ? board.height() - 2 : 1;

        int rowDiff = to.row() - from.row();
        int colDiff = Math.abs(to.col() - from.col());

        if (colDiff == 0) {
            if (rowDiff == direction) {
                return board.isEmpty(to);
            }
            if (from.row() == startRow && rowDiff == 2 * direction) {
                return board.isEmpty(to) && board.isEmpty(from.offset(direction, 0));
            }
        } else if (colDiff == 1 && rowDiff == direction) {
            return board.isOccupiedByOpponent(to, pawn.color());
        }
        return false;
    }
}