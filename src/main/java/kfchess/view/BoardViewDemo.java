package kfchess.view;

import kfchess.model.Position;
import java.util.Map;
import javax.swing.Timer;
import java.util.HashMap;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;



public class BoardViewDemo {


public static void main(String[] args) throws InterruptedException{
    String boardPath = "src/main/resources/board.png"; 

    Img board = new Img().read(boardPath);

    BoardGeometry geometry = new BoardGeometry(board.get().getWidth(), board.get().getHeight(), 8, 8);
    Map<Position, Piece> dummyPieces = new HashMap<>();
    dummyPieces.put(new Position(0, 0), new Piece(PieceColor.BLACK, PieceKind.ROOK));
    dummyPieces.put(new Position(7, 7), new Piece(PieceColor.WHITE, PieceKind.ROOK));
    dummyPieces.put(new Position(0, 4), new Piece(PieceColor.BLACK, PieceKind.KING));
    dummyPieces.put(new Position(7, 4), new Piece(PieceColor.WHITE, PieceKind.KING));

         
    BoardView view = new BoardView(board, geometry);
    view.render(dummyPieces, 0);

    long startTime = System.currentTimeMillis();
    Timer timer = new Timer(100, e -> {
        long elapsedTime = System.currentTimeMillis() - startTime;
        view.render(dummyPieces, elapsedTime);
    });

    timer.start();
}
}