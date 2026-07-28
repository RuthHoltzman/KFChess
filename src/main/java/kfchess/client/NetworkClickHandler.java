package kfchess.client;

import kfchess.input.BoardMapper;
import kfchess.model.Position;
import kfchess.view.GameSceneView;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import java.awt.Rectangle;
import java.util.Optional;

/**
 * מטפל בקליק/קליק-ימני על הלוח במצב רשת - הוצא מ-NetworkGameWindow
 * (היה שם כמתודה פרטית) כדי שחישוב "איפה בדיוק נלחץ" יהיה נגיש לבדיקה
 * בנפרד, בלי Swing/רשת אמיתיים. זו בדיוק הלוגיקה שההערה ב-
 * BoardLayoutCalculator מזהירה עליה כמקור לשני באגים קודמים ("קליק לא
 * במקום") - ומקביל למה ש-GameCommandController (kfchess.engine, הקונטרולר
 * המקביל בצד השרת) כבר עושה: מחלקה קטנה שרק מנתבת קליק, בלי לוגיקת משחק בעצמה.
 * <p>
 * שני תפקידים נפרדים בכוונה: resolvePosition() טהורה לגמרי (בלי Swing,
 * בלי רשת) - נבדקת ישירות ב-NetworkClickHandlerTest. handle() היא
 * השכבה הדקה שעוטפת אותה בבדיקת gameOver ובשליחה בפועל ל-GameClient -
 * לא נבדקת ישירות בהצלחה (תלויה בחיבור רשת אמיתי), באותה גישה בדיוק כמו
 * HomeScreen.connect()/LoginScreenMain.handleLogin() שלא נבדקות
 * ישירות בעוד buildUri()/validate() כן.
 * <p>
 * מחזיקה גם GameSceneView (רק לשם שאילת restartButtonBounds()) - לא
 * משוכפלים כאן המספרים של מיקום/גודל כפתור ה-Restart; GameSceneView
 * נשאר המקור היחיד לאמת עליהם (אותו עיקרון בדיוק שבגללו BoardLayoutCalculator
 * קיים בכלל).
 */
public class NetworkClickHandler {

    private final GameClient client;
    private final BoardMapper boardMapper;
    private final GameSceneView sceneView;

    public NetworkClickHandler(GameClient client, BoardMapper boardMapper, GameSceneView sceneView) {
        this.client = client;
        this.boardMapper = boardMapper;
        this.sceneView = sceneView;
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

    // מטפל בקליק בפועל: כש-gameOver, הקליק היחיד שרלוונטי הוא על כפתור
    // ה-Restart (ר' handleRestartClick) - שום קליק/קפיצה רגילים לא
    // אמורים לקרות אז. אחרת, פותר את המיקום (resolvePosition) ושולח
    // CLICK/JUMP לשרת. לא נוגע במנוע בכלל - אין GameEngine מקומי במצב
    // רשת, השרת הוא היחיד שמחליט אם הפעולה חוקית.
    public void handle(int pixelX, int pixelY, BoardLayout layout, boolean gameOver, boolean isJump) {
        if (gameOver) {
            handleRestartClick(pixelX, pixelY, layout);
            return;
        }
        resolvePosition(pixelX, pixelY, layout).ifPresent(position -> {
            if (isJump) {
                client.sendJump(position.row(), position.col());
            } else {
                client.sendClick(position.row(), position.col());
            }
        });
    }

    // בודקת אם קליק (בזמן gameOver) פגע בכפתור ה-Restart שמצויר על הלוח -
    // ממירה לפיקסל יחסי-ללוח (כמו resolvePosition) ובודקת מול
    // GameSceneView.restartButtonBounds() (שם היחיד שבאמת יודע איפה
    // הכפתור). פגיעה => שולח RESTART לשרת; שני הצדדים צריכים לשלוח כדי
    // שהלוח יתאפס בפועל (ר' GameSession.applyRestartVote).
    private void handleRestartClick(int pixelX, int pixelY, BoardLayout layout) {
        int boardX = pixelX - layout.offsetX();
        int boardY = pixelY - layout.offsetY();
        Rectangle button = sceneView.restartButtonBounds();
        if (button.contains(boardX, boardY)) {
            client.sendRestart();
        }
    }
}
