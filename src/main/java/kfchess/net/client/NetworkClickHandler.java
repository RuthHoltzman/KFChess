package kfchess.net.client;

import kfchess.input.BoardMapper;
import kfchess.model.Position;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import java.util.Optional;

/**
 * מטפל בקליק/קליק-ימני על הלוח במצב רשת - הוצא מ-NetworkGameWindowMain
 * (היה שם כמתודה פרטית) כדי שחישוב "איפה בדיוק נלחץ" יהיה נגיש לבדיקה
 * בנפרד, בלי Swing/רשת אמיתיים. זו בדיוק הלוגיקה שההערה ב-
 * BoardLayoutCalculator מזהירה עליה כמקור לשני באגים קודמים ("קליק לא
 * במקום") - ומקביל למה ש-NetworkActions (kfchess.engine) כבר עושה בצד
 * השרת: מחלקה קטנה שרק מנתבת קליק, בלי לוגיקת משחק בעצמה.
 * <p>
 * שני תפקידים נפרדים בכוונה: resolvePosition() טהורה לגמרי (בלי Swing,
 * בלי רשת) - נבדקת ישירות ב-NetworkClickHandlerTest. handle() היא
 * השכבה הדקה שעוטפת אותה בבדיקת gameOver ובשליחה בפועל ל-GameClient -
 * לא נבדקת ישירות (תלויה בחיבור רשת אמיתי), באותה גישה בדיוק כמו
 * HomeScreenMain.connect()/LoginScreenMain.handleLogin() שלא נבדקות
 * ישירות בעוד buildUri()/validate() כן.
 */
public class NetworkClickHandler {

    private final GameClient client;
    private final BoardMapper boardMapper;

    public NetworkClickHandler(GameClient client, BoardMapper boardMapper) {
        this.client = client;
        this.boardMapper = boardMapper;
    }

    // ממירה פיקסל מוחלט (יחסית לכל תוכן החלון) למיקום לוגי על הלוח, או
    // Optional.empty() אם הקליק נפל מחוץ ללוח עצמו (בפאנל צד, או בשוליים
    // הריקים סביב לוח ממורכז לא-ריבועי - ר' BoardLayoutCalculator).
    public Optional<Position> resolvePosition(int pixelX, int pixelY, BoardLayout layout) {
        int boardX = pixelX - layout.offsetX();
        int boardY = pixelY - layout.offsetY();
        if (boardX < 0 || boardX >= layout.boardPixelSize() || boardY < 0 || boardY >= layout.boardPixelSize()) {
            return Optional.empty();
        }
        return Optional.of(boardMapper.pixelToPosition(boardX, boardY, layout.cellSize(), layout.cellSize()));
    }

    // מטפל בקליק בפועל: בודק gameOver, פותר את המיקום (resolvePosition),
    // ואם התקבל מיקום תקין שולח CLICK/JUMP לשרת. לא נוגע במנוע בכלל -
    // אין GameEngine מקומי במצב רשת, השרת הוא היחיד שמחליט אם הפעולה חוקית.
    public void handle(int pixelX, int pixelY, BoardLayout layout, boolean gameOver, boolean isJump) {
        if (gameOver) {
            return; // אין עדיין כפתור Restart במצב רשת - ר' "הצעד הבא" ב-PROGRESS.md
        }
        resolvePosition(pixelX, pixelY, layout).ifPresent(position -> {
            if (isJump) {
                client.sendJump(position.row(), position.col());
            } else {
                client.sendClick(position.row(), position.col());
            }
        });
    }
}
