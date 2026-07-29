package kfchess.rules;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shows two things: (1) movement rules can be tested in complete isolation - just Board and
 * RuleEngine, no GameEngine and no IO; (2) a custom movement rule can be injected at runtime
 * and is genuinely enforced.
 */
class RuleEngineTest {

    @Test
    void rookMove_blockedPath_isIllegal() {
        Board board = Board.createDefault(3, 3);
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK);
        Piece blocker = new Piece(PieceColor.WHITE, PieceKind.PAWN);
        board.placePiece(new Position(0, 0), rook);
        board.placePiece(new Position(0, 1), blocker);

        RuleEngine ruleEngine = new RuleEngine();

        assertFalse(ruleEngine.isLegalMove(board, rook, new Position(0, 0), new Position(0, 2)));
    }

    @Test
    void rookMove_clearPath_isLegal() {
        Board board = Board.createDefault(3, 3);
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK);
        board.placePiece(new Position(0, 0), rook);

        RuleEngine ruleEngine = new RuleEngine();

        assertTrue(ruleEngine.isLegalMove(board, rook, new Position(0, 0), new Position(0, 2)));
    }

    @Test
    void customPieceRule_canBeInjectedDynamically() {
        Board board = Board.createDefault(3, 3);
        Piece customPiece = new Piece(PieceColor.WHITE, PieceKind.KNIGHT);
        board.placePiece(new Position(1, 1), customPiece);

        RuleEngine ruleEngine = new RuleEngine();
        // Dummy rule: the piece may only move to (0,0) - an example of a rule
        // a user could inject at runtime for a custom piece.
        ruleEngine.registerCustomRule(PieceKind.KNIGHT,
                (b, from, to) -> to.equals(new Position(0, 0)));

        assertTrue(ruleEngine.isLegalMove(board, customPiece, new Position(1, 1), new Position(0, 0)));
        assertFalse(ruleEngine.isLegalMove(board, customPiece, new Position(1, 1), new Position(2, 2)));
    }
}
