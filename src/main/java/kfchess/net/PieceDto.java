package kfchess.net;

import kfchess.model.Piece;
import kfchess.model.Position;

/**
 * מצמיד כלי למיקומו על הלוח - Piece בכוונה לא יודע את מיקומו בעצמו
 * (Board הוא מקור האמת היחיד למיקום, ר' תיעוד Piece.java), אז זו
 * העטיפה הדקה ביותר האפשרית שעדיין נותנת ללקוח את שניהם יחד. Gson
 * מסריאלז את piece/position ישירות (color/kind/state/row/col) בלי
 * שצריך להעתיק אף שדה ידנית.
 */
public class PieceDto {

    private final Piece piece;
    private final Position position;

    public PieceDto(Piece piece, Position position) {
        this.piece = piece;
        this.position = position;
    }

    public static PieceDto from(Piece piece, Position position) {
        return new PieceDto(piece, position);
    }

    // צריך רק בצד הלקוח - כדי לקרוא בחזרה מה ש-Gson פיענח מ-JSON נכנס
    // (ר' ClientSnapshotReconstructor). השרת ממשיך להשתמש רק ב-from().
    public Piece piece() {
        return piece;
    }

    public Position position() {
        return position;
    }
}
