package kfchess.view;

import kfchess.model.Board;

/**
 * Abstraction משותפת לכל תצוגת לוח גרפית עתידית (שכבה 6 - Renderer).
 * ImageView (וכל מימוש גרפי עתידי) מממש ממשק זה - כך ש-Main יוכל
 * בעתיד להזריק כל מימוש בלי לדעת אם מדובר בטקסט או בגרפיקה.
 * אין קשר בין ממשק זה לבין BoardPrinter (שכבת Text I/O) - שתי
 * האחריויות נפרדות, גם אם הן נראו פעם דומות.
 */
public interface BoardView {
    void render(Board board);
}