package kfchess.model;

/**
 * המצב שכלי בודד יכול להיות בו. שימו לב שהמצב נשמר per-piece
 * (ולא כמשתנה סטטי גלובלי כמו בקוד המקורי) - זה מה שמאפשר
 * למספר כלים לזוז/לקפוץ בו-זמנית (concurrent movement).
 */
public enum PieceState {
    IDLE,
    IN_TRANSIT,
    JUMPING
}
