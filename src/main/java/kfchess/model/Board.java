package kfchess.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The board-state abstraction: an interface so Rules/Engine/IO/View depend only on this contract, not on ArrayBoard's internals. */
public interface Board {

    int height();

    int width();

    /** The piece at this position, if any. */
    Optional<Piece> pieceAt(Position pos);

    boolean isWithinBounds(Position pos);

    default boolean isEmpty(Position pos) {
        return pieceAt(pos).isEmpty();
    }

    default boolean isOccupiedByOpponent(Position pos, PieceColor movingColor) {
        return pieceAt(pos).map(p -> p.color() != movingColor).orElse(false);
    }

    default boolean isOccupiedBySameColor(Position pos, PieceColor movingColor) {
        return pieceAt(pos).map(p -> p.color() == movingColor).orElse(false);
    }

    /** Every piece currently on the board. */
    List<Piece> allPieces();

    /** Moves whatever piece is at "from" to "to"; throws if "from" is empty. */
    void movePieceTo(Position from, Position to);

    void removePieceAt(Position pos);

    /** Overwrites whatever is at "pos" with a new piece (e.g. pawn promotion). */
    void replacePieceAt(Position pos, Piece newPiece);

    void placePiece(Position pos, Piece piece);

    /** Factory method: the one place to touch to swap the default Board implementation. */
    static Board createDefault(int height, int width) {
        return new ArrayBoard(height, width);
    }
}

/** Map-backed Board implementation; package-private so it's only reachable through the Board interface. */
class ArrayBoard implements Board {

    private final int height;
    private final int width;
    private final Map<Position, Piece> pieces = new HashMap<>();

    ArrayBoard(int height, int width) {
        this.height = height;
        this.width = width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public Optional<Piece> pieceAt(Position pos) {
        return Optional.ofNullable(pieces.get(pos));
    }

    @Override
    public boolean isWithinBounds(Position pos) {
        return pos.isWithinBounds(height, width);
    }

    @Override
    public List<Piece> allPieces() {
        return List.copyOf(pieces.values());
    }

    @Override
    public void movePieceTo(Position from, Position to) {
        Piece piece = pieces.remove(from);
        if (piece == null) {
            throw new IllegalStateException("No piece at " + from);
        }
        pieces.put(to, piece);
    }

    @Override
    public void removePieceAt(Position pos) {
        pieces.remove(pos);
    }

    @Override
    public void replacePieceAt(Position pos, Piece newPiece) {
        pieces.put(pos, newPiece);
    }

    @Override
    public void placePiece(Position pos, Piece piece) {
        pieces.put(pos, piece);
    }
}
