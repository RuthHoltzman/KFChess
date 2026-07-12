package kfchess.view;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.Position;

import java.util.Optional;

/**
 * Abstraction משותפת לכל תצוגת לוח אפשרית. חבילתית (package-private)
 * בכוונה - כדי לא ליצור קובץ נוסף, ובו-זמנית לאפשר גם ל-ImageView
 * (שנמצא באותה חבילה) לממש אותה. GameEngine/Controller יכולים בעתיד
 * להזריק כל מימוש של BoardView בלי לדעת אם מדובר בטקסט או בגרפיקה.
 */
interface BoardView {
    void render(Board board);
}

/**
 * תצוגה טקסטואלית של הלוח למסך - המימוש היחיד היום של BoardView.
 * תלויה רק בממשק הציבורי של Board, כך שאם ייצוג הלוח הפנימי ישתנה
 * (למשל לבינארי), הקוד כאן לא ידע ולא יצטרך להשתנות.
 */
public class Renderer implements BoardView {

    private static final String EMPTY_CELL_TOKEN = ".";
    private static final String CELL_SEPARATOR = " ";

    @Override
    public void render(Board board) {
        StringBuilder output = new StringBuilder();
        for (int row = 0; row < board.height(); row++) {
            output.append(renderRow(board, row)).append(System.lineSeparator());
        }
        System.out.print(output);
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
