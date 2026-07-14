package kfchess.io;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.Position;

import java.util.Optional;

/**
 * מדפיסה את מצב הלוח הלוגי הנוכחי בצורתו הקנונית (canonical form) -
 * אותו פורמט טוקנים שה-BoardParser קורא (".", "wK", "bP" וכו').
 * תלויה רק בממשק הציבורי של Board/Piece, כך שאם ייצוג הלוח הפנימי
 * ישתנה (למשל לבינארי) - הקוד כאן לא ידע ולא יצטרך להשתנות.
 * שייכת לשכבת Text I/O: אין כאן שום חוק משחק, שינוי Board, או פירוש קלט.
 */
public class BoardPrinter {

    private static final String EMPTY_CELL_TOKEN = ".";
    private static final String CELL_SEPARATOR = " ";

    public void print(Board board) {
        System.out.print(toCanonicalText(board));
    }

    private String toCanonicalText(Board board) {
        StringBuilder output = new StringBuilder();
        for (int row = 0; row < board.height(); row++) {
            output.append(renderRow(board, row)).append(System.lineSeparator());
        }
        return output.toString();
    }

    private String renderRow(Board board, int row) {
        StringBuilder rowBuilder = new StringBuilder();
        for (int col = 0; col < board.width(); col++) {
            rowBuilder.append(renderCell(board, new Position(row, col)));
            if (col < board.width() - 1) {
                rowBuilder.append(CELL_SEPARATOR);
            }
        }
        return rowBuilder.toString();
    }

    private String renderCell(Board board, Position pos) {
        Optional<Piece> piece = board.pieceAt(pos);
        return piece.map(Piece::toString).orElse(EMPTY_CELL_TOKEN);
    }
}