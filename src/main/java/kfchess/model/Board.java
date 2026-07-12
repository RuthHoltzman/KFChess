package kfchess.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Board הוא ה-abstraction המרכזי של מצב הלוח.
 * <p>
 * זהו ממשק (Interface) ולא מחלקה קונקרטית בכוונה: כל שאר המערכת
 * (Rules, Engine, IO, View) תלויה רק בחתימת הממשק הזה ולא במימוש הפנימי.
 * היום המימוש היחיד ({@link ArrayBoard}) שומר את הכלים ב-Map&lt;Position, Piece&gt;,
 * אבל בעתיד אפשר להוסיף מימוש חדש (למשל BinaryBoard מבוסס bitboards) שמממש
 * את אותו הממשק - בלי לשנות שורת קוד אחת ב-Rules/Engine/IO/View.
 */
public interface Board {

    int height();

    int width();

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

    List<Piece> allPieces();

    void movePieceTo(Position from, Position to);

    void removePieceAt(Position pos);

    void replacePieceAt(Position pos, Piece newPiece);

    void placePiece(Position pos, Piece piece);

    /**
     * Factory method - נקודת ההרחבה היחידה שצריך לגעת בה כדי להחליף
     * ייצוג בעתיד (למשל להחזיר BinaryBoard תחת דגל קונפיגורציה).
     */
    static Board createDefault(int height, int width) {
        return new ArrayBoard(height, width);
    }
}

/**
 * מימוש טקסטואלי/אובייקטי של Board, מבוסס Map&lt;Position, Piece&gt;.
 * מוצהר כמחלקה חבילתית (package-private, ללא מילת מפתח public) בתוך
 * אותו הקובץ של הממשק - כדי לא ליצור קובץ חדש, ובו-זמנית לשמור
 * שהיא לא נגישה ישירות מחוץ לחבילת model (רק דרך הממשק Board).
 */
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
