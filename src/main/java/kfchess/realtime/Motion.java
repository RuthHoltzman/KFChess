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
    private final long startTime;
    private final long arrivalTime;

    public Motion(Piece piece, Position from, Position to, long startTime, long arrivalTime) {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.startTime = startTime;
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

    public long startTime() {
        return startTime;
    }

    public long arrivalTime() {
        return arrivalTime;
    }

    public boolean hasArrived(long clock) {
        return clock >= arrivalTime;
    }

    /**
     * שבר ההתקדמות (0..1) של התנועה, לפי "עכשיו" נתון. משמש את שכבת
     * ה-UI כדי לצייר את הכלי "הולך" בהדרגה בין from ל-to, במקום
     * "לקפוץ" ישר ליעד ברגע שהמהלך מסתיים.
     */
    public double progress(long now) {
        long duration = arrivalTime - startTime;
        if (duration <= 0) {
            return 1.0;
        }
        double p = (now - startTime) / (double) duration;
        if (p < 0) return 0.0;
        if (p > 1) return 1.0;
        return p;
    }
}
