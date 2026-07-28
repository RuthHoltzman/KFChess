package kfchess.view.layout;

import kfchess.view.Img;

import java.awt.Dimension;

/**
 * מוציא את חישוב הגיאומטריה של הלוח על המסך למקום אחד, נפרד מחלון
 * המשחק עצמו (NetworkGameWindow) - במקור נכתב כדי לשתף קוד גיאומטריה
 * בין חלון המשחק המקומי (שהוסר) לחלון הרשת, ונשאר מחלקה עצמאית ונבדקת
 * גם אחרי ההסרה, כדי שחישוב הגיאומטריה יישאר מופרד מהציור/מה-Swing
 * עצמו + פרמטרי (כמו רוחב פאנל הצד) בלי hard-code.
 */
public final class BoardLayoutCalculator {

    private static final int MIN_CELL_SIZE = 20;

    private BoardLayoutCalculator() {
    }

    /**
     * כל המספרים שקובעים "איפה כל דבר נמצא על המסך" ברגע נתון - מחושבים
     * *במקום אחד בלבד* (computeLayout למטה) ומועברים מוכנים לכל מי שצריך
     * אותם (רינדור, טיפול בקליק). זה בדיוק הלקח משתי הבעיות הקודמות: כל
     * פעם ששני מקומות חישבו משהו דומה בנפרד, הם התבדרו זה מזה.
     */
    public record BoardLayout(int cellSize, int boardPixelSize, int offsetX, int offsetY) {}

    /**
     * הלוח תמיד *ריבועי* - cellSize זהה לרוחב ולגובה, לא שני מספרים
     * נפרדים. אם החלון עצמו לא ריבועי, לוקחים את הצד הקטן מבין השניים
     * (השטח שנשאר באמצע, אחרי הפאנלים) לקביעת גודל הלוח, וממרכזים אותו -
     * כך שנשארים שוליים ריקים בציר שיש בו עודף מקום, במקום למתוח את
     * הלוח למלבן.
     */
    public static BoardLayout computeLayout(Dimension content, int cols, int rows, int sidePanelWidth) {
        int middleWidth = Math.max(1, content.width - sidePanelWidth * 2);
        int middleHeight = Math.max(1, content.height);
        int squareRawSize = Math.min(middleWidth, middleHeight);

        int cellSize = Math.max(MIN_CELL_SIZE, squareRawSize / Math.max(cols, rows));
        int boardPixelSize = cellSize * Math.max(cols, rows);

        int offsetX = sidePanelWidth + (middleWidth - boardPixelSize) / 2;
        int offsetY = (middleHeight - boardPixelSize) / 2;
        return new BoardLayout(cellSize, boardPixelSize, offsetX, offsetY);
    }

    /** גודל התוכן הנוכחי של החלון, או גודל התחלתי קבוע אם החלון עוד לא נפתח בפועל (ר' Img.isReady). */
    public static Dimension currentContentSize(Img windowAnchor, int sidePanelWidth, int initialCellSize) {
        if (!windowAnchor.isReady()) {
            return new Dimension(sidePanelWidth * 2 + initialCellSize * 8, initialCellSize * 8);
        }
        return windowAnchor.contentSize();
    }
}
