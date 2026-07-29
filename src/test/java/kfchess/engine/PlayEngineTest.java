package kfchess.engine;

import kfchess.bus.EventBus;
import kfchess.model.Board;
import kfchess.model.PlayState;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the "capture in mid-air" case (see PlayEngine.captureFailsAgainstJumpingDefender):
 * piece A moves to capture piece B, but B is JUMPING - so A vanishes and B survives intact.
 * The reported bug was that nobody scored; now the defender who effectively did the capturing
 * is credited with the attacker's piece value.
 * <p>
 * Builds a PlayEngine directly, with no PlaySession or network, using a simulated clock so time
 * can be advanced without a real Thread.sleep.
 */
class PlayEngineTest {

    private static PlayEngine newEngine(Board board) {
        return new PlayEngine(new PlayState(board), new RuleEngine(), new RaelTime(), new EventBus());
    }

    @Test
    void tick_attackerMovesOntoJumpingDefender_defenderGetsAttackersPieceValueAsScore() {
        Board board = Board.createDefault(3, 3);
        Piece attacker = new Piece(PieceColor.WHITE, PieceKind.ROOK); // value()==5
        Piece defender = new Piece(PieceColor.BLACK, PieceKind.PAWN);
        board.placePiece(new Position(0, 0), attacker);
        board.placePiece(new Position(0, 1), defender);
        PlayEngine engine = newEngine(board);

        engine.beginJump(defender); // defender starts jumping (1000ms)
        engine.tryMove(attacker, new Position(0, 0), new Position(0, 1)); // attacker moves onto the jumping defender (1 square, 1000ms travel)

        engine.handleWait(1000); // arrival == jump end; arrivals resolve before jump-expiry (see engine comment)

        assertEquals(5, engine.scores().get(PieceColor.BLACK));
        assertEquals(0, engine.scores().get(PieceColor.WHITE));
    }

    @Test
    void tick_attackerMovesOntoJumpingDefender_attackerIsRemovedAndDefenderSurvives() {
        Board board = Board.createDefault(3, 3);
        Piece attacker = new Piece(PieceColor.WHITE, PieceKind.ROOK);
        Piece defender = new Piece(PieceColor.BLACK, PieceKind.PAWN);
        board.placePiece(new Position(0, 0), attacker);
        board.placePiece(new Position(0, 1), defender);
        PlayEngine engine = newEngine(board);

        engine.beginJump(defender);
        engine.tryMove(attacker, new Position(0, 0), new Position(0, 1));
        engine.handleWait(1000);

        assertTrue(board.pieceAt(new Position(0, 0)).isEmpty());
        assertEquals(defender, board.pieceAt(new Position(0, 1)).orElse(null));
    }

    @Test
    void tick_normalCaptureWithoutJumping_stillCreditsTheMovingSideAsBefore() {
        // Regression: an ordinary capture (defender not jumping) must be unaffected -
        // the attacker still scores, exactly as before the fix.
        Board board = Board.createDefault(3, 3);
        Piece attacker = new Piece(PieceColor.WHITE, PieceKind.ROOK);
        Piece defender = new Piece(PieceColor.BLACK, PieceKind.PAWN);
        board.placePiece(new Position(0, 0), attacker);
        board.placePiece(new Position(0, 1), defender);
        PlayEngine engine = newEngine(board);

        engine.tryMove(attacker, new Position(0, 0), new Position(0, 1));
        engine.handleWait(1000);

        assertEquals(1, engine.scores().get(PieceColor.WHITE));
        assertEquals(0, engine.scores().get(PieceColor.BLACK));
        assertTrue(board.pieceAt(new Position(0, 1)).isPresent());
        assertEquals(attacker, board.pieceAt(new Position(0, 1)).orElse(null));
    }
}
