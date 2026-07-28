package kfchess.engine;

import kfchess.bus.EventBus;
import kfchess.model.Board;
import kfchess.model.Game;
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
 * רות דיווחה: "כשכלי בא לאכול כלי אחר ובסוף הכלי המותקף קופץ ואוכל את
 * התוקף הוא לא מקבל נקודות" - כלומר תרחיש "לכידה באוויר" (ר' GameEngine.
 * captureFailsAgainstJumpingDefender): כלי א' זז לתפוס את כלי ב', אבל
 * ב' נמצא במצב JUMPING - א' "מתאדה" (נמחק) ו-ב' נשאר שלם. לפני התיקון:
 * אף אחד לא קיבל נקודות. אחרי התיקון: ב' (המגן/ת שבפועל תפס/ה) מקבל/ת
 * את ערך הכלי של א'.
 * <p>
 * בונה GameEngine ישירות (בלי GameSession/רשת בכלל, ר' RuleEngineTest
 * לאותה גישה) - עם RaelTime מדומה כדי "לקפוץ" בזמן בלי Thread.sleep
 * אמיתי, בדיוק כמו ש-GameSessionTest עושה עם tick().
 */
class GameEngineTest {

    private static GameEngine newEngine(Board board) {
        return new GameEngine(new Game(board), new RuleEngine(), new RaelTime(), new EventBus());
    }

    @Test
    void tick_attackerMovesOntoJumpingDefender_defenderGetsAttackersPieceValueAsScore() {
        Board board = Board.createDefault(3, 3);
        Piece attacker = new Piece(PieceColor.WHITE, PieceKind.ROOK); // value()==5
        Piece defender = new Piece(PieceColor.BLACK, PieceKind.PAWN);
        board.placePiece(new Position(0, 0), attacker);
        board.placePiece(new Position(0, 1), defender);
        GameEngine engine = newEngine(board);

        engine.handleJump(new Position(0, 1)); // defender starts jumping (1000ms)
        engine.handleClick(new Position(0, 0)); // select attacker
        engine.handleClick(new Position(0, 1)); // attacker moves onto the jumping defender (1 square, 1000ms travel)

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
        GameEngine engine = newEngine(board);

        engine.handleJump(new Position(0, 1));
        engine.handleClick(new Position(0, 0));
        engine.handleClick(new Position(0, 1));
        engine.handleWait(1000);

        assertTrue(board.pieceAt(new Position(0, 0)).isEmpty());
        assertEquals(defender, board.pieceAt(new Position(0, 1)).orElse(null));
    }

    @Test
    void tick_normalCaptureWithoutJumping_stillCreditsTheMovingSideAsBefore() {
        // רגרסיה: תפיסה רגילה (המגן/ת *לא* קופץ/ת) לא אמורה להשתנות בכלל -
        // עדיין התוקף/ת מקבל/ת נקודות, בדיוק כמו לפני התיקון.
        Board board = Board.createDefault(3, 3);
        Piece attacker = new Piece(PieceColor.WHITE, PieceKind.ROOK);
        Piece defender = new Piece(PieceColor.BLACK, PieceKind.PAWN);
        board.placePiece(new Position(0, 0), attacker);
        board.placePiece(new Position(0, 1), defender);
        GameEngine engine = newEngine(board);

        engine.handleClick(new Position(0, 0));
        engine.handleClick(new Position(0, 1));
        engine.handleWait(1000);

        assertEquals(1, engine.scores().get(PieceColor.WHITE));
        assertEquals(0, engine.scores().get(PieceColor.BLACK));
        assertTrue(board.pieceAt(new Position(0, 1)).isPresent());
        assertEquals(attacker, board.pieceAt(new Position(0, 1)).orElse(null));
    }
}
