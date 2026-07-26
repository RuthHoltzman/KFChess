package kfchess.view;

import kfchess.engine.snapshot.GameSnapshot;
import kfchess.model.PieceColor;
import kfchess.server.ClientRole;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

/**
 * שכבת התצוגה העליונה: מרכיבה קנבס אחד גדול -
 * פאנל השחקן הלבן (מוצמד לקצה השמאלי) | הלוח (ריבועי, ממורכז בשטח
 * שנשאר באמצע) | פאנל השחקן השחור (מוצמד לקצה הימני) -
 * ורק היא קוראת ל-show() בפועל.
 * <p>
 * בכוונה, המחלקה הזו לא מחשבת שום גיאומטריה בעצמה יותר (לא היכן הלוח
 * מתחיל, לא כמה מקום נשאר) - כל המספרים (גודל הלוח, ה-offset שלו)
 * מגיעים כפרמטרים מוכנים מ-NetworkGameWindowMain, שהוא המקום היחיד שבאמת
 * יודע מה גודל החלון האמיתי כרגע. זה לקח משתי באגים קודמים: כל פעם
 * ששני מקומות שונים חישבו את אותו מספר בנפרד (במקום שאחד יחשב ויעביר
 * לשני), הם התבדרו זה מזה וזה יצר בדיוק את הבאגים של "קליק לא במקום".
 * <p>
 * הלוח *תמיד* ריבועי (cellSize זהה לרוחב ולגובה) - זו הסיבה שאין יותר
 * "קצוות שהופכות למלבן": אם החלון עצמו לא ריבועי, פשוט נשאר שוליים
 * ריקים (letterboxing) בציר שיש בו עודף מקום, במקום למתוח את הלוח.
 */
public class GameSceneView {

    private static final Color OUTER_BACKGROUND = new Color(30, 30, 30);
    private static final Color OVERLAY_BACKGROUND = new Color(0, 0, 0, 150);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color BUTTON_COLOR = new Color(46, 139, 87);
    private static final Color BUTTON_BORDER_COLOR = Color.WHITE;
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;
    // כתום-אדמדם, שונה בכוונה מ-OVERLAY_BACKGROUND (שחור) - כדי שהבאנר
    // יבלוט כ"אזהרה" ולא יתבלבל עם מסך ה-Game-Over, למרות שהם אף פעם
    // לא מוצגים בו-זמנית בפועל (ר' תיעוד drawDisconnectBanner).
    private static final Color DISCONNECT_BANNER_BACKGROUND = new Color(120, 40, 20, 210);
    // כחול רגוע, שונה בכוונה מהכתום-אדמדם של ניתוק - זו לא "אזהרה" (אף
    // אחד לא עשה משהו רע), רק מידע נייטרלי "עוד לא התחלנו". ר' תיעוד
    // drawWaitingForOpponentBanner.
    private static final Color WAITING_BANNER_BACKGROUND = new Color(20, 60, 110, 210);
    // אפור-כחלחל נייטרלי לפס שם-החדר - שונה מכל שאר הבאנרים (לא אזהרה,
    // לא "עוד לא התחלנו") כי הוא לא תלוי-מצב בכלל, תמיד מוצג בדיוק אותו
    // דבר (בקשת רות - שם החדר "בפס עליון קבוע").
    private static final Color ROOM_HEADER_BACKGROUND = new Color(45, 45, 55);

    private static final int TITLE_FONT_SIZE = 42;
    private static final int SUBTITLE_FONT_SIZE = 20;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 56;
    private static final int BUTTON_FONT_SIZE = 22;
    // גובה/גודל-פונט משותפים לשני סוגי הבאנר העליון (ניתוק/המתנה ליריב) -
    // אותה גיאומטריה בדיוק, רק צבע/טקסט שונים לפי המצב. אלה מצוירים *על
    // הלוח עצמו* (boardCanvas) ותלויים-מצב - בניגוד ל-ROOM_HEADER_HEIGHT
    // למטה, שהוא פס *קבוע* לרוחב כל הסצנה (כולל שני הפאנלים), לא רק הלוח.
    private static final int TOP_BANNER_HEIGHT = 40;
    private static final int TOP_BANNER_FONT_SIZE = 20;
    // גובה פס שם-החדר הקבוע - נפרד בכוונה מ-TOP_BANNER_HEIGHT (אלה שני
    // סוגי-פס שונים לגמרי: זה קבוע ולרוחב מלא, האחרים תלויי-מצב ולרוחב
    // הלוח בלבד). NetworkGameWindowMain *חייב* להשתמש באותו מספר בדיוק
    // (ר' roomHeaderHeight() למטה) כשהוא מקטין את השטח הפנוי ללוח/פאנלים -
    // בדיוק העיקרון שכבר קיים ב-BoardLayoutCalculator ("חישוב במקום אחד,
    // לא בשני מקומות שיתבדרו זה מזה").
    private static final int ROOM_HEADER_HEIGHT = 34;
    private static final int ROOM_HEADER_FONT_SIZE = 18;

    private final BoardView boardView;
    private final SidePanelView sidePanelView;
    // שלושת השדות הבאים (roomId/role/username) קבועים לכל אורך חיי החלון -
    // בניגוד לכל שאר המידע שמגיע ל-render() (GameSnapshot), הם *לא*
    // משתנים תוך כדי משחק (שם החדר/התפקיד/שם המשתמש נקבעים פעם אחת ברגע
    // החיבור, ר' NetworkGameWindowMain.launch) - אז הם שדות של הבנאי, לא
    // פרמטרים חדשים ב-render() (שהיה משנה את החתימה שלה בלי צורך אמיתי).
    private final String roomId;
    private final ClientRole role;
    private final String username;

    // "הגודל האחרון שידוע" - מתעדכן בתחילת כל render(). לא זיכרון-מצב
    // אמיתי, רק נוחות כדי ש-restartButtonBounds() (בלי פרמטרים, נקראת
    // גם מחוץ ל-render כדי לבדוק קליק) תדע למה להתייחס.
    private int lastBoardPixelSize;

    // חתימה ישנה (בלי roomId/role/username) - נשארת כדי ש-NetworkClickHandlerTest
    // הקיים ימשיך לעבוד בלי שינוי (הוא בונה GameSceneView רק כדי לשאול
    // restartButtonBounds(), לא קורא ל-render() בכלל - ר' תיעוד הטסט).
    // שקולה ל-roomId=null/role=null/username=null, כמו שדפוס התאימות-
    // לאחור הזה כבר עובד בכל הפרויקט (ר' SnapshotMessage/GameSession וכו').
    public GameSceneView(BoardView boardView, int panelWidth) {
        this(boardView, panelWidth, null, null, null);
    }

    public GameSceneView(BoardView boardView, int panelWidth, String roomId, ClientRole role, String username) {
        this.boardView = boardView;
        this.sidePanelView = new SidePanelView(panelWidth);
        this.roomId = roomId;
        this.role = role;
        this.username = username;
    }

    // כמה מקום (בפיקסלים) צריך לשמור *מלכתחילה* בשביל פס שם-החדר, לפני
    // שמחשבים איפה הלוח/פאנלים בכלל נכנסים - NetworkGameWindowMain קורא
    // לזה לפני BoardLayoutCalculator.computeLayout (ר' תיעוד שם) כדי
    // שהלוח לא "יגלוש" מתחת לפס הזה. public+static בכוונה (בניגוד לשאר
    // הקבועים הפרטיים כאן) - זה המספר היחיד מהמחלקה הזו שגם קוד מבחוץ
    // חייב לדעת, כדי לא לשכפל אותו כקבוע נפרד שם (בדיוק הבאג ששני באגי-
    // "קליק לא במקום" הקודמים נבעו ממנו).
    public static int roomHeaderHeight() {
        return ROOM_HEADER_HEIGHT;
    }

    /**
     * מיקום/גודל כפתור ה-Restart, ביחס ללוח בלבד (0,0 = הפינה השמאלית-
     * עליונה של הלוח עצמו, לא של כל הסצנה) - נכון תמיד אחרי לפחות
     * render() אחד. הקוד הקורא צריך להחסיר את ה-offset של הלוח (שהוא
     * עצמו מחשב) לפני שהוא בודק קליק מול זה.
     * <p>
     * הערה: כרגע אין קוד שקורא למתודה הזו בפועל (שימשה את מסך המשחק
     * המקומי שהוסר) - נשארה כאן כתשתית מוכנה לכפתור Restart ב-UI
     * הרשת, אם/כשיתווסף.
     */
    public Rectangle restartButtonBounds() {
        int x = (lastBoardPixelSize - BUTTON_WIDTH) / 2;
        int y = lastBoardPixelSize / 2 + 30;
        return new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    /**
     * @param sceneWidthPx   הרוחב הכולל של החלון (הפנימי, לציור) - כולל שני הפאנלים.
     * @param sceneHeightPx  הגובה הכולל של החלון.
     * @param boardPixelSize גודל הלוח בפיקסלים - *ריבוע* אחד (רוחב=גובה תמיד).
     * @param boardOffsetX   היכן הלוח מתחיל בציר X בתוך הסצנה (כבר כולל את הפאנל השמאלי + מירכוז).
     * @param boardOffsetY   היכן הלוח מתחיל בציר Y בתוך הסצנה (מירכוז אנכי אם יש שוליים).
     */
    public void render(GameSnapshot snapshot, int sceneWidthPx, int sceneHeightPx,
                        int boardPixelSize, int boardOffsetX, int boardOffsetY) {
        this.lastBoardPixelSize = boardPixelSize;

        BoardGeometry geometry = new BoardGeometry(
                boardPixelSize, boardPixelSize, snapshot.boardHeightCells(), snapshot.boardWidthCells());

        Img scene = new Img().newCanvas(sceneWidthPx, sceneHeightPx, OUTER_BACKGROUND);
        // תמיד מצויר, ראשון (בקשת רות - שם החדר "בפס עליון קבוע", לא תלוי-
        // מצב כמו הבאנרים למטה) - לרוחב *כל* הסצנה (כולל שני הפאנלים),
        // כי זה מידע כללי על המשחק, לא ספציפי ללוח. boardOffsetY שמתקבל
        // כפרמטר כבר "יודע" להזיז את הלוח למטה בגובה הזה בדיוק - ר' תיעוד
        // roomHeaderHeight()/NetworkGameWindowMain.computeBoardLayout.
        drawRoomHeader(scene, sceneWidthPx);

        Img boardCanvas = boardView.render(snapshot, geometry);
        if (snapshot.gameOver()) {
            drawGameOverOverlay(boardCanvas, snapshot.winner(), snapshot.restartRequestedByViewer());
        }
        // בפועל אף פעם לא קורה בו-זמנית עם gameOver (ר' GameSession.resolveExpiredDisconnects -
        // ברגע שחלון החסד פג, gameOver הופך ל-true ו-disconnectSecondsRemaining חוזר ל-null
        // באותו טיק) - אבל אין תלות מפורשת בין שני ה-if-ים כאן בכוונה, כל אחד עצמאי לגמרי
        // לפי מה שה-snapshot בפועל מכיל, ולא לפי הנחה על מה "לא אמור" לקרות יחד.
        if (snapshot.disconnectSecondsRemaining() != null) {
            drawDisconnectBanner(boardCanvas, snapshot.disconnectSecondsRemaining());
        }
        // אף פעם לא קורה בו-זמנית עם disconnectSecondsRemaining (ר' תיעוד
        // GameSession.isWaitingForOpponent - "פנוי" דורש שלא יהיה חלון-חסד
        // פתוח על הצד השני, אז שני התנאים סותרים זה את זה) - שוב, if
        // עצמאי בכוונה, לא תלוי בתנאי הקודם.
        if (snapshot.waitingForOpponent()) {
            drawWaitingForOpponentBanner(boardCanvas);
        }
        boardCanvas.drawOn(scene, boardOffsetX, boardOffsetY);

        // שני הפאנלים מתחילים מתחת לפס שם-החדר (startY=ROOM_HEADER_HEIGHT)
        // ולא מ-0 כמו קודם - כדי שלא "יצטיירו" מתחת לפס ההוא (ר' תיעוד
        // SidePanelView.draw). isLocalPlayer - true בדיוק לפאנל שמתאים
        // ל-role של הלקוח הזה עצמו (WHITE→פאנל שמאל, BLACK→פאנל ימין) -
        // לצופה/ה (role==SPECTATOR, או role==null בחתימת-התאימות הישנה)
        // אף אחד מהשניים לא "שלי", אז אף פאנל לא מקבל את התג "(You: ...)".
        int panelWidth = sidePanelView.panelWidth();
        int panelStartY = ROOM_HEADER_HEIGHT;
        int panelHeight = sceneHeightPx - ROOM_HEADER_HEIGHT;
        sidePanelView.draw(scene, 0, panelStartY, panelHeight,
                PieceColor.WHITE,
                snapshot.scores().getOrDefault(PieceColor.WHITE, 0),
                snapshot.moveLog().getOrDefault(PieceColor.WHITE, List.of()),
                role == ClientRole.WHITE, username);

        sidePanelView.draw(scene, sceneWidthPx - panelWidth, panelStartY, panelHeight,
                PieceColor.BLACK,
                snapshot.scores().getOrDefault(PieceColor.BLACK, 0),
                snapshot.moveLog().getOrDefault(PieceColor.BLACK, List.of()),
                role == ClientRole.BLACK, username);

        scene.show();
    }

    /**
     * מציירת מסך "נגמר המשחק": רקע כהה חצי-שקוף, כותרת עם שם המנצח,
     * וכפתור Restart. restartRequestedByViewer - האם *הצופה הזה בדיוק*
     * כבר ביקש/ה RESTART (ר' GameSession.applyRestartVote - שני הצדדים
     * צריכים לבקש כדי שהלוח יתאפס בפועל) - אם כן, מציגה "Waiting for
     * opponent..." במקום "Game Over"/"Restart", כדי שהצד שכבר לחץ יידע
     * שהקליק שלו נקלט ולא רק ילחץ שוב ושוב בלי משוב.
     */
    private void drawGameOverOverlay(Img boardCanvas, String winner, boolean restartRequestedByViewer) {
        boardCanvas.fillRect(0, 0, lastBoardPixelSize, lastBoardPixelSize, OVERLAY_BACKGROUND);

        int centerX = lastBoardPixelSize / 2;
        int titleBaselineY = lastBoardPixelSize / 2 - 50;

        String title = winner == null ? "Game Over" : (winner + " Wins!");
        int titleWidth = boardCanvas.textWidth(title, TITLE_FONT_SIZE, true);
        boardCanvas.drawText(title, centerX - titleWidth / 2, titleBaselineY, TITLE_FONT_SIZE, TITLE_COLOR, true);

        String subtitle = restartRequestedByViewer ? "Waiting for opponent..." : "Game Over";
        int subtitleWidth = boardCanvas.textWidth(subtitle, SUBTITLE_FONT_SIZE, false);
        boardCanvas.drawText(subtitle, centerX - subtitleWidth / 2, titleBaselineY + 30,
                SUBTITLE_FONT_SIZE, TITLE_COLOR, false);

        Rectangle button = restartButtonBounds();
        boardCanvas.fillRect(button.x, button.y, button.width, button.height, BUTTON_COLOR);
        boardCanvas.drawRect(button.x, button.y, button.width, button.height, BUTTON_BORDER_COLOR, 2);

        String buttonText = restartRequestedByViewer ? "Waiting..." : "Restart";
        int buttonTextWidth = boardCanvas.textWidth(buttonText, BUTTON_FONT_SIZE, true);
        int buttonTextX = button.x + (button.width - buttonTextWidth) / 2;
        int buttonTextY = button.y + button.height / 2 + BUTTON_FONT_SIZE / 3;
        boardCanvas.drawText(buttonText, buttonTextX, buttonTextY, BUTTON_FONT_SIZE, BUTTON_TEXT_COLOR, true);
    }

    /**
     * מציירת פס אזהרה צר לרוחב הלוח כולו, צמוד לקצה העליון: "Opponent
     * disconnected - Xs to reconnect" - שלב 5 (auto-resign), ר' תיעוד
     * GameSession.pendingDisconnects/DISCONNECT_GRACE_MILLIS. בכוונה
     * *לא* overlay מלא כמו drawGameOverOverlay - המשחק לא נגמר, הלוח
     * עדיין אמור להיראות (קפוא, כי אין tick חדש עד שהניתוק נפתר, אבל
     * לא מוסתר) - רק באנר דק שמסביר *למה* הוא קפוא.
     */
    private void drawDisconnectBanner(Img boardCanvas, int secondsRemaining) {
        boardCanvas.fillRect(0, 0, lastBoardPixelSize, TOP_BANNER_HEIGHT, DISCONNECT_BANNER_BACKGROUND);

        String text = "Opponent disconnected - " + secondsRemaining + "s to reconnect";
        int textWidth = boardCanvas.textWidth(text, TOP_BANNER_FONT_SIZE, true);
        int textX = (lastBoardPixelSize - textWidth) / 2;
        int textY = TOP_BANNER_HEIGHT / 2 + TOP_BANNER_FONT_SIZE / 3;
        boardCanvas.drawText(text, textX, textY, TOP_BANNER_FONT_SIZE, TITLE_COLOR, true);
    }

    /**
     * מציירת פס עליון (אותה גיאומטריה בדיוק כמו drawDisconnectBanner, רק
     * צבע כחול נייטרלי): "Waiting for an opponent to join..." - בקשת רות
     * (הסבב הזה) - כל עוד GameSession.isWaitingForOpponent() (רק צד אחד
     * מחובר), כדי שהשחקן/ית היחיד/ה שכבר בפנים ידע/תדע *למה* קליקים לא
     * עושים כלום (ר' GameSession.applyCommand - נחסמים בשקט בלי הודעה).
     */
    private void drawWaitingForOpponentBanner(Img boardCanvas) {
        boardCanvas.fillRect(0, 0, lastBoardPixelSize, TOP_BANNER_HEIGHT, WAITING_BANNER_BACKGROUND);

        String text = "Waiting for an opponent to join...";
        int textWidth = boardCanvas.textWidth(text, TOP_BANNER_FONT_SIZE, true);
        int textX = (lastBoardPixelSize - textWidth) / 2;
        int textY = TOP_BANNER_HEIGHT / 2 + TOP_BANNER_FONT_SIZE / 3;
        boardCanvas.drawText(text, textX, textY, TOP_BANNER_FONT_SIZE, TITLE_COLOR, true);
    }

    /**
     * מציירת פס עליון *קבוע* לרוחב כל הסצנה (בניגוד לשני הבאנרים למעלה,
     * שמצוירים רק על הלוח ורק בתנאים מסוימים) - בקשת רות: "שם חדר" תמיד
     * גלוי על המסך, לא רק בכותרת החלון (Img.setTitle, שנשארת גם היא ללא
     * שינוי - זה תוסף, לא תחליף). roomId==null (מקרה-קצה: ROLE_ASSIGNED
     * לא הגיעה בזמן, ר' HomeScreenMain.waitForAssignedGameId) מוצג כ-"?"
     * במקום לזרוק/להציג "null" מילולית.
     * <p>
     * תפקיד/שם המשתמש *לא* מוצגים כאן עבור WHITE/BLACK - אלה מופיעים
     * בפאנל הצד המתאים (ר' SidePanelView.draw, isLocalPlayer) לפי בקשת
     * רות ("גם וגם"). עבור SPECTATOR דווקא כן מוצגים כאן - אין לצופה/ה
     * "פאנל שלו/ה" ששני הצדדים הקיימים (WHITE/BLACK) לא שייכים לו בכלל.
     */
    private void drawRoomHeader(Img scene, int sceneWidthPx) {
        scene.fillRect(0, 0, sceneWidthPx, ROOM_HEADER_HEIGHT, ROOM_HEADER_BACKGROUND);

        String text = "Room: " + (roomId == null ? "?" : roomId);
        if (role == ClientRole.SPECTATOR) {
            text += "   |   Spectator" + (username != null ? ": " + username : "");
        }
        int textWidth = scene.textWidth(text, ROOM_HEADER_FONT_SIZE, true);
        int textX = (sceneWidthPx - textWidth) / 2;
        int textY = ROOM_HEADER_HEIGHT / 2 + ROOM_HEADER_FONT_SIZE / 3;
        scene.drawText(text, textX, textY, ROOM_HEADER_FONT_SIZE, TITLE_COLOR, true);
    }
}
