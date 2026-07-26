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

    private static final Color BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER = new Color(120, 120, 120);
    private static final Color HEADER_TEXT = new Color(20, 20, 20);
    private static final Color MOVE_TEXT = new Color(60, 60, 60);

    private static final int PADDING = 14;
    private static final int HEADER_FONT_SIZE = 22;
    private static final int SCORE_FONT_SIZE = 18;
    private static final int MOVE_FONT_SIZE = 15;
    private static final int MOVE_LINE_HEIGHT = 22;
    private static final int HEADER_Y = 32;
    private static final int SCORE_Y = 62;
    private static final int MOVES_TITLE_Y = 96;
    private static final int MOVES_START_Y = 120;

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
    // ערכים מוחלטים - כדי שהפאנל עצמו יישאר "טיפש" וזז שלם ביחד. isLocalPlayer
    // + username (בקשת רות - להציג תפקיד+שם המשתמש) מתווספים לתוך שורת
    // הכותרת הקיימת ("White (You: ruth)") במקום שורה נפרדת - כך שאין שום
    // שינוי בגובה/מיקום שאר האלמנטים (ניקוד/מהלכים) בין הפאנל "שלי" לפאנל
    // של היריב/ה, אין סיכון לחוסר-התאמה חזותית בין השניים. אין overload
    // ישן שנשאר: ל-SidePanelView יש קריאה אחת בלבד (GameSceneView.render),
    // בניגוד ל-DTOs שנשלחים ברשת שחייבים תאימות לאחור.
    public void draw(Img canvas, int offsetX, int startY, int panelHeight,
                      PieceColor color, int score, List<String> moves,
                      boolean isLocalPlayer, String username) {
        canvas.fillRect(offsetX, startY, panelWidth, panelHeight, BACKGROUND);
        canvas.drawRect(offsetX, startY, panelWidth, panelHeight, BORDER, 2);

        String title = displayName(color);
        if (isLocalPlayer) {
            title += username != null ? " (You: " + username + ")" : " (You)";
        }
        canvas.drawText(title, offsetX + PADDING, startY + HEADER_Y, HEADER_FONT_SIZE, HEADER_TEXT, true);
        canvas.drawText("Score: " + score, offsetX + PADDING, startY + SCORE_Y, SCORE_FONT_SIZE, HEADER_TEXT, false);
        canvas.drawText("Moves:", offsetX + PADDING, startY + MOVES_TITLE_Y, SCORE_FONT_SIZE, HEADER_TEXT, true);

        int maxVisibleRows = Math.max(0, (panelHeight - MOVES_START_Y - PADDING) / MOVE_LINE_HEIGHT);
        List<String> recentMoves = lastN(moves, maxVisibleRows);

        // המהלך האחרון מוצג ראשון (למעלה) - זה מה שהכי מעניין את השחקן
        // ברגע נתון, ואין צורך בגלילה כי הפאנל ממילא מוגבל בגובה קבוע.
        int y = startY + MOVES_START_Y;
        for (int i = recentMoves.size() - 1; i >= 0; i--) {
            canvas.drawText(recentMoves.get(i), offsetX + PADDING, y, MOVE_FONT_SIZE, MOVE_TEXT, false);
            y += MOVE_LINE_HEIGHT;
        }
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
