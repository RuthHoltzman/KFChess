package kfchess.protocol;

import kfchess.model.Piece;
import kfchess.model.Position;



/** Pairs a piece with its board position, since Piece deliberately doesn't know its own location. */
public record PieceDto(Piece piece, Position position) {

    /** Builds a PieceDto from a piece and the position it was found at. */
    public static PieceDto from(Piece piece, Position position) {
        return new PieceDto(piece, position);
    }
}
