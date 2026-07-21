package kfchess.engine.snapshot;

import kfchess.model.Piece;
import kfchess.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * עוקב אחרי אפקטי "לכידה" זמניים - קיים אך ורק בשביל אנימציית דהייה
 * ב-UI (למשל טבעת דוהה במקום שכלי נעלם ממנו). אין לזה שום השפעה על
 * חוקי המשחק - הוצא מ-GameEngine כדי שהאחריות הזו (מה מציירים) לא
 * תגדיל את מחלקת חוקי המשחק.
 */
public class CaptureEffectTracker {

    public static final long CAPTURE_EFFECT_DURATION_MS = 450;

    private final List<CaptureEffect> recentCaptures = new ArrayList<>();

    /** רושם שכלי הוסר הרגע מהלוח, לצורך אפקט הלכידה הקצר ב-UI. */
    public void register(Piece removedPiece, Position at, long now) {
        recentCaptures.add(new CaptureEffect(removedPiece.kind(), removedPiece.color(), at, now));
    }

    /** מנקה אפקטי לכידה שכבר עברו את משך החיים שלהם (ר' CAPTURE_EFFECT_DURATION_MS). */
    public void purgeExpired(long now) {
        recentCaptures.removeIf(effect -> now - effect.removedAt() >= CAPTURE_EFFECT_DURATION_MS);
    }

    /** עותק הגנתי של אפקטי הלכידה הפעילים כרגע. */
    public List<CaptureEffect> active() {
        return List.copyOf(recentCaptures);
    }
}
