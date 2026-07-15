package kfchess.view;

import kfchess.engine.GameSnapshot;
import kfchess.engine.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.model.*;
import javax.swing.Timer;
import java.util.HashMap;
import java.util.Map;

public class BoardViewDemo {
    public static void main(String[] args) {
        Img board = new Img().read("src/main/resources/board.png");
        BoardGeometry geometry = new BoardGeometry(board.get().getWidth(), board.get().getHeight(), 8, 8);

        Map<Position, Piece> dummyPieces = new HashMap<>();
        dummyPieces.put(new Position(0, 0), new Piece(PieceColor.BLACK, PieceKind.ROOK));
        dummyPieces.put(new Position(7, 7), new Piece(PieceColor.WHITE, PieceKind.ROOK));
        dummyPieces.put(new Position(0, 4), new Piece(PieceColor.BLACK, PieceKind.KING));
        dummyPieces.put(new Position(7, 4), new Piece(PieceColor.WHITE, PieceKind.KING));

        BoardView view = new BoardView(board, geometry);
        SnapshotFactory snapshotFactory = new SnapshotFactory(
                geometry.getCellWidth(), geometry.getCellHeight(), 8, 8);

        // רינדור ראשון מיידי - מעלה את ה-EDT ופותח את החלון
        GameSnapshot firstSnapshot = snapshotFactory.createSnapshot(
                dummyPieces, System.currentTimeMillis(), null, false, null);
        view.render(firstSnapshot);

        Timer timer = new Timer(100, e -> {
            long now = System.currentTimeMillis();
            GameSnapshot snapshot = snapshotFactory.createSnapshot(
                    dummyPieces, now, null, false, null);
            view.render(snapshot);
        });
        timer.start();
        BoardMapper mapper = BoardMapper.withDefaultCellSize(); // או לוודא שהגודל תואם ל-geometry
        javax.swing.SwingUtilities.invokeLater(() -> {
    board.onClick((x, y) -> {
        Position clicked = mapper.pixelToPosition(x, y);
        System.out.println("Clicked pixel (" + x + "," + y + ") -> " + clicked);
    });
});
    }

}