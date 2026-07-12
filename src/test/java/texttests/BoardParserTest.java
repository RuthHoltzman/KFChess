package texttests;

import kfchess.io.BoardParser;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * דוגמה לבדיקות יחידה עם הזרקת תלויות נקייה (Dependency Injection):
 * במקום System.in אנחנו מזריקים ל-BoardParser Scanner שקורא ממחרוזת
 * שנמצאת בזיכרון. אין כאן monkey patching ואין שינוי זמן-ריצה של
 * הקוד הנבדק - רק החלפת מקור הקלט דרך הבנאי, בדיוק כי BoardParser
 * תוכנן מלכתחילה לקבל Scanner מבחוץ ולא ליצור אחד לעצמו.
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
    void readBoard_mismatchedRowWidths_returnsNull() {
        String input = "Board:\n" +
                "bR bN bB\n" +
                "bR bN\n" +
                "Commands:\n";

        Board board = new BoardParser(new Scanner(input)).readBoard();

        assertNull(board);
    }

    @Test
    void readBoard_unknownToken_returnsNull() {
        String input = "Board:\n" +
                "bX . .\n" +
                "Commands:\n";

        Board board = new BoardParser(new Scanner(input)).readBoard();

        assertNull(board);
    }

    @Test
    void readBoard_missingBoardSection_returnsNull() {
        String input = "Commands:\n";

        Board board = new BoardParser(new Scanner(input)).readBoard();

        assertNull(board);
    }

    @Test
    void readBoard_emptyInput_returnsNull() {
        Board board = new BoardParser(new Scanner("")).readBoard();

        assertNull(board);
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