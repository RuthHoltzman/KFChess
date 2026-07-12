package texttests;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import kfchess.rules.RuleEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * מדגימה שני דברים: (1) שאפשר לבדוק חוקי תנועה בבידוד מוחלט, בלי
 * GameEngine ובלי IO בכלל - רק Board + RuleEngine. (2) שאפשר להזריק
 * חוק תנועה מותאם אישית בזמן ריצה (דרישה 6ב') ולבדוק שהוא באמת נאכף.
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
        // חוק דמה: הכלי יכול "לזוז" רק לתא (0,0) - בדיוק כדוגמה לחוק
        // דינמי שיוזרק בעתיד ע"י המשתמש עבור כלי מותאם אישית.
        ruleEngine.registerCustomRule(PieceKind.KNIGHT,
                (b, from, to) -> to.equals(new Position(0, 0)));

        assertTrue(ruleEngine.isLegalMove(board, customPiece, new Position(1, 1), new Position(0, 0)));
        assertFalse(ruleEngine.isLegalMove(board, customPiece, new Position(1, 1), new Position(2, 2)));
    }
}
