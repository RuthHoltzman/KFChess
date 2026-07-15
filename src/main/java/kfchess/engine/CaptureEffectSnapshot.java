package kfchess.engine;

import kfchess.model.PieceColor;
import kfchess.model.PieceKind;

/**
 * גרסת התצוגה של CaptureEffect - כמו PieceSnapshot לכלים חיים, רק
 * לאפקט לכידה זמני: כבר עם קואורדינטות פיקסלים מוכנות (לפי המשבצת
 * שבה הכלי הוסר) ושבר התקדמות (progress) של 0 (הרגע שנתפס) עד 1
 * (האפקט הסתיים ועומד להיעלם) - כדי ש-BoardView רק יצטרך לצייר, בלי
 * לחשב תזמונים בעצמו.
 */
public record CaptureEffectSnapshot(
        PieceKind kind,
        PieceColor color,
        double pixelX,
        double pixelY,
        double progress
) {}
