package kfchess.view;

import kfchess.model.PieceColor;

import java.awt.Color;
import java.util.List;

/**
 * מציירת פאנל צד אחד (ניקוד + רשימת מהלכים) של שחקן בודד, על קנבס נתון
 * ובהיסט X נתון. כל הציור עובר דרך Img בלבד (fillRect/drawRect/drawText) -
 * בהתאם לדרישה שלא להשתמש בשום ספריית גרפיקה מלבד ה-class הזה.
 * <p>
 * הפאנל "טיפש" בכוונה: הוא לא יודע כלום על GameEngine/GameSnapshot,
 * רק מקבל ערכים מוכנים (צבע, ניקוד, רשימת מהלכים) ומצייר אותם.
 */
public class SidePanelView {

    // בקשת רות (עיצוב יותר יפה, "צבעים/עיצוב שונים לכל אזור") - שני
    // "ערכות נושא" נפרדות במקום צבע אחיד לשני הפאנלים כמו קודם: בהיר-חם
    // ל-White, כהה ל-Black - כדי שהעין תבדיל מיד בין הצדדים, לא רק לפי
    // הכיתוב "White"/"Black". נבחר לפי הפרמטר color שכבר קיים ב-draw()
    // (ר' WHITE_THEME/BLACK_THEME למטה) - אין צורך בפרמטר נוסף.
    private record Theme(Color background, Color border, Color headerText, Color moveText,
                          Color badgeBackground, Color badgeText) {}

    private static final Theme WHITE_THEME = new Theme(
            new Color(246, 243, 236), new Color(200, 190, 170),
            new Color(44, 44, 42), new Color(95, 94, 90),
            new Color(232, 223, 200), new Color(95, 77, 31));

    private static final Theme BLACK_THEME = new Theme(
            new Color(35, 35, 35), new Color(70, 70, 68),
            new Color(232, 230, 223), new Color(168, 166, 158),
            new Color(60, 74, 92), new Color(205, 222, 240));

    private static final int PADDING = 14;
    private static final int HEADER_FONT_SIZE = 22;
    private static final int BADGE_FONT_SIZE = 12;
    private static final int SCORE_FONT_SIZE = 18;
    private static final int MOVE_FONT_SIZE = 15;
    private static final int MOVE_LINE_HEIGHT = 22;
    private static final int HEADER_Y = 32;
    // "פילה" (rounded badge) ל-"You: username" - מתחת לכותרת, לא בתוכה
    // (בקשת רות - הטקסט הקודם שהוצמד לכותרת "נחתך/נהדק מדי"). התג עצמו
    // מצויר רק אם isLocalPlayer, אבל השטח *מתחת* לכותרת נשאר שמור קבוע
    // (SCORE_Y/MOVES_*_Y לא משתנים בין הפאנל "שלי" לפאנל של היריב/ה) -
    // כדי ששני הפאנלים יישארו מיושרים אחד מול השני בדיוק, בלי קשר למי
    // מהם "שלי" כרגע.
    private static final int BADGE_TOP_Y = 40;
    private static final int BADGE_HEIGHT = 18;
    private static final int SCORE_Y = 72;
    private static final int MOVES_TITLE_Y = 106;
    private static final int MOVES_START_Y = 130;

    private final int panelWidth;

    public SidePanelView(int panelWidth) {
        this.panelWidth = panelWidth;
    }

    public int panelWidth() {
        return panelWidth;
    }

    // startY נוסף (בקשת רות - שם חדר בפס עליון קבוע, ר' GameSceneView):
    // כל הפאנל צריך לזוז למטה באותו גובה בדיוק כשיש פס כזה, כדי לא להצטייר
    // מתחת לו - startY=0 שקול בדיוק להתנהגות הישנה (בלי פס עליון בכלל).
    // כל הקבועים (HEADER_Y/SCORE_Y/וכו') הם היסטים *יחסיים* ל-startY, לא
    // ערכים מוחלטים - כדי שהפאנל עצמו יישאר "טיפש" וזז שלם ביחד.
    // isLocalPlayer+username (בקשת רות - להציג תפקיד+שם המשתמש) מציגים
    // "פילה" (badge) מעוגלת מתחת לכותרת - במקום התוספת הקודמת בתוך שורת
    // הכותרת עצמה ("White (You: ruth)"), שרות תיארה כ"נחתך/נהדק" - השטח
    // שמתחתיה נשאר שמור קבוע גם כשלא מוצגת (ר' BADGE_TOP_Y/SCORE_Y למעלה),
    // כדי שהפאנל של היריב/ה יישאר מיושר בדיוק מול הפאנל "שלי". אין overload
    // ישן שנשאר: ל-SidePanelView יש קריאה אחת בלבד (GameSceneView.render),
    // בניגוד ל-DTOs שנשלחים ברשת שחייבים תאימות לאחור.
    public void draw(Img canvas, int offsetX, int startY, int panelHeight,
                      PieceColor color, int score, List<String> moves,
                      boolean isLocalPlayer, String username) {
        Theme theme = color == PieceColor.WHITE ? WHITE_THEME : BLACK_THEME;

        canvas.fillRect(offsetX, startY, panelWidth, panelHeight, theme.background());
        canvas.drawRect(offsetX, startY, panelWidth, panelHeight, theme.border(), 2);

        String title = displayName(color);
        canvas.drawText(title, offsetX + PADDING, startY + HEADER_Y, HEADER_FONT_SIZE, theme.headerText(), true);

        if (isLocalPlayer) {
            drawYouBadge(canvas, offsetX, startY, theme, username);
        }

        canvas.drawText("Score: " + score, offsetX + PADDING, startY + SCORE_Y, SCORE_FONT_SIZE, theme.headerText(), false);
        canvas.drawText("Moves:", offsetX + PADDING, startY + MOVES_TITLE_Y, SCORE_FONT_SIZE, theme.headerText(), true);

        int maxVisibleRows = Math.max(0, (panelHeight - MOVES_START_Y - PADDING) / MOVE_LINE_HEIGHT);
        List<String> recentMoves = lastN(moves, maxVisibleRows);

        // המהלך האחרון מוצג ראשון (למעלה) - זה מה שהכי מעניין את השחקן
        // ברגע נתון, ואין צורך בגלילה כי הפאנל ממילא מוגבל בגובה קבוע.
        int y = startY + MOVES_START_Y;
        for (int i = recentMoves.size() - 1; i >= 0; i--) {
            canvas.drawText(recentMoves.get(i), offsetX + PADDING, y, MOVE_FONT_SIZE, theme.moveText(), false);
            y += MOVE_LINE_HEIGHT;
        }
    }

    // מציירת את הפילה "You" / "You: username" - רוחב מחושב מהטקסט עצמו
    // (לא קבוע קשיח) כדי שהיא תתאים גם לשמות משתמש קצרים וגם ארוכים,
    // בדיוק העיקרון שכבר משמש את כפתור ה-Restart (textWidth לפני ציור).
    private void drawYouBadge(Img canvas, int offsetX, int startY, Theme theme, String username) {
        String text = username != null ? "You: " + username : "You";
        int textWidth = canvas.textWidth(text, BADGE_FONT_SIZE, true);
        int badgeWidth = textWidth + PADDING;
        int badgeY = startY + BADGE_TOP_Y;
        canvas.fillRoundRect(offsetX + PADDING, badgeY, badgeWidth, BADGE_HEIGHT,
                BADGE_HEIGHT, BADGE_HEIGHT, theme.badgeBackground());
        int textY = badgeY + BADGE_HEIGHT - 5;
        canvas.drawText(text, offsetX + PADDING + PADDING / 2, textY, BADGE_FONT_SIZE, theme.badgeText(), true);
    }

    private static String displayName(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }

    private static List<String> lastN(List<String> list, int n) {
        if (n <= 0 || list.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, list.size() - n);
        return list.subList(from, list.size());
    }
}
