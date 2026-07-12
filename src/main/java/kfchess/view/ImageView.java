package kfchess.view;

import kfchess.model.Board;

/**
 * שלד למימוש עתידי של תצוגה גרפית (מבוססת תמונות/פיקסלים במקום טקסט).
 * מממש את אותו BoardView כמו Renderer - כך ש-Controller יוכל בעתיד
 * להזריק ImageView במקום Renderer מבלי שאף קוד אחר במערכת ישתנה.
 * זו בדיוק אותה עקרון האבסטרקציה שביקשתם עבור Board, מיושם גם בצד התצוגה.
 */
public class ImageView implements BoardView {

    @Override
    public void render(Board board) {
        throw new UnsupportedOperationException("Graphical rendering is not implemented yet");
    }
}
