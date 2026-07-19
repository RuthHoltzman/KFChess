package kfchess.engine;

import kfchess.model.Piece;

/**
 * ייצוג "ויזואלי" של קפיצה פעילה: איזה כלי קופץ, מתי הקפיצה התחילה
 * ומתי היא צפויה להסתיים. GameEngine כבר עוקב אחרי זמן הסיום לצורך
 * הלוגיקה (jumpEndTimes) - הרשומה הזו רק חושפת גם את זמן ההתחלה
 * כלפי חוץ, כדי ש-SnapshotFactory יוכל לחשב שבר התקדמות (0..1) בתוך
 * הקפיצה ולצייר עליו קשת גובה (הכלי "עולה" ו"יורד"), בדיוק כמו
 * שהוא כבר עושה עם Motion לתנועה רגילה.
 */
public record JumpVisual(Piece piece, long startTime, long endTime) {}
