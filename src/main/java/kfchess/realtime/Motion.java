package kfchess.realtime;

import kfchess.model.Piece;
import kfchess.model.Position;

/**
 * מייצג מהלך של כלי בודד שנמצא "בדרך" ליעד שלו - מהמקור, ליעד,
 * עם זמן הגעה משוער. GameEngine מחזיק רשימה של Motion פעילים
 * (אחד לכל כלי שזז כרגע), במקום המשתנים הסטטיים הגלובליים היחידים
 * שהיו בקוד המקורי - וכך כמה כלים יכולים לזוז בו-זמנית.
 */
public final class Motion {

    private final Piece piece;
    private final Position from;
    private final Position to;
    private final long arrivalTime;

    public Motion(Piece piece, Position from, Position to, long arrivalTime) {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.arrivalTime = arrivalTime;
    }

    public Piece piece() {
        return piece;
    }

    public Position from() {
        return from;
    }

    public Position to() {
        return to;
    }

    public long arrivalTime() {
        return arrivalTime;
    }

    public boolean hasArrived(long clock) {
        return clock >= arrivalTime;
    }
}
