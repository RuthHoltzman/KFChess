package kfchess.engine;

import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;

/**
 * "זיכרון" קצר-טווח של כלי שהוסר מהלוח הרגע - בין אם זו לכידה רגילה
 * (התוקף ניצח והמגן הוסר ביעד), ובין אם זו לכידה כושלת מול כלי קופץ
 * (התוקף עצמו "מתאדה" ומוסר מהמקור). ה-Board לא שומר שום זיכרון של
 * כלים שהוסרו (זה תפקידו - להחזיק רק את המצב הנוכחי), ולכן GameEngine
 * שומר את זה בנפרד, לחלון זמן קצר (ר' GameEngine.CAPTURE_EFFECT_DURATION_MS),
 * רק כדי שה-UI יוכל לצייר אפקט "נלכד" קצר במקום שבו הכלי נעלם - אחרת
 * ההיעלמות קורית "בין פריימים" ונראית כאילו לא קרה כלום.
 */
public record CaptureEffect(PieceKind kind, PieceColor color, Position at, long removedAt) {}
