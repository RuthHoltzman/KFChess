package kfchess.io;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * An example of clean dependency injection in a unit test: instead of System.in, the parser is
 * given a Scanner over an in-memory string. No monkey patching and no runtime rewriting - just a
 * different input source through the constructor, which is exactly what BoardParser was designed for.
 */

class BoardParserTest {

    @Test
    void readBoard_validInput_buildsBoardWithCorrectPieces() {
        String input = "Board:\n" +
                "bR bN . . . . bN bR\n" +
                ". . . . . . . .\n" +
                "Commands:\n";

        Board board = new BoardParser(new Scanner(input)).readBoard();

        assertNotNull(board);
        assertEquals(8, board.width());
        assertEquals(2, board.height());

        Piece rook = board.pieceAt(new Position(0, 0)).orElseThrow();
        assertEquals(PieceColor.BLACK, rook.color());
        assertEquals(PieceKind.ROOK, rook.kind());

        assertTrue(board.isEmpty(new Position(0, 2)));
    }

    @Test
    void readBoard_singleWhitePawn_isNotBlack() {
        String input = "Board:\n" +
                "wP\n" +
                "Commands:\n";

        Board board = new BoardParser(new Scanner(input)).readBoard();

        Piece pawn = board.pieceAt(new Position(0, 0)).orElseThrow();
        assertFalse(pawn.color() == PieceColor.BLACK);
    }
}