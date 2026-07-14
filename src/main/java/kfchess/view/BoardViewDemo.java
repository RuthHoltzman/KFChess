package kfchess.view;

import kfchess.model.Position;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;

public class BoardViewDemo {

    private static String imagePathFor(Piece piece) {
    String folder = "" + piece.kind().code() + Character.toUpperCase(piece.color().code());
    return "src/main/resources/pieces/" + folder + "/states/idle/sprites/1.png";
    }  
public static void main(String[] args) {
    String boardPath = "src/main/resources/board.png"; 

    Img board = new Img().read(boardPath);

    BoardGeometry geometry = new BoardGeometry(board.get().getWidth(), board.get().getHeight(), 8, 8);
    Map<Position, Piece> dummyPieces = new HashMap<>();
    dummyPieces.put(new Position(0, 0), new Piece(PieceColor.BLACK, PieceKind.ROOK));
    dummyPieces.put(new Position(7, 7), new Piece(PieceColor.WHITE, PieceKind.ROOK));
    dummyPieces.put(new Position(0, 4), new Piece(PieceColor.BLACK, PieceKind.KING));
    dummyPieces.put(new Position(7, 4), new Piece(PieceColor.WHITE, PieceKind.KING));

         
   Dimension cellSize = new Dimension(geometry.getCellWidth(), geometry.getCellHeight());

    for (Map.Entry<Position, Piece> entry : dummyPieces.entrySet()) {
        Position pos = entry.getKey();
        Piece piece = entry.getValue();

        Img pieceImg = new Img().read(imagePathFor(piece), cellSize, true, null);
        Point p = geometry.cellToPixel(pos);
        pieceImg.drawOn(board, p.x, p.y);
    }

    board.show();
        }
}
