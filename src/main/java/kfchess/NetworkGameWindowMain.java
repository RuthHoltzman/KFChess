package kfchess;

import com.google.gson.Gson;
import kfchess.engine.snapshot.GameSnapshot;
import kfchess.engine.snapshot.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.model.Board;
import kfchess.server.client.ClientSnapshotReconstructor;
import kfchess.server.client.GameClient;
import kfchess.server.client.IncomingMessageSummary;
import kfchess.server.client.IncomingSnapshot;
import kfchess.server.client.NetworkClickHandler;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.Img;
import kfchess.view.layout.BoardLayoutCalculator;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import javax.swing.Timer;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

/**
 * חלון Swing במצב רשת - בלי GameEngine מקומי בכלל: כל מצב המשחק מגיע
 * מהשרת (ר' GameClient), עובר שחזור (ClientSnapshotReconstructor) חזרה
 * לאובייקטי תחום אמיתיים, ואז מצויר דרך SnapshotFactory/GameSceneView -
 * זו ההחלטה שתועדה מראש ב-PROGRESS.md ("קוד הציור נשאר משותף").
 * <p>
 * קליק/קליק-ימני לא נוגעים במנוע בכלל - הם רק שולחים CLICK/JUMP לשרת
 * (GameClient.sendClick/sendJump); השרת הוא היחיד שמחליט אם הפעולה
 * חוקית (כולל דחיית קליקים של צופה - ר' GameSession.applyCommand),
 * ולכן אין כאן שום כפילות של הבדיקה הזו בצד הלקוח.
 */
public class NetworkGameWindowMain {

    private static final int INITIAL_CELL_SIZE = 100;
    private static final int SIDE_PANEL_WIDTH = 240;
    // גודל לוח זמני, רק כדי שהחלון יוכל להיפתח *לפני* שמתקבל snapshot
    // ראשון מהשרת - מוחלף מיד ברגע שמגיעה הודעה אמיתית (ר' boardWidthCells
    // /boardHeightCells שכבר מגיעים ברשת, לא hard-code בפועל).
    private static final int PLACEHOLDER_BOARD_SIZE = 8;

    // פותחת את חלון המשחק עבור לקוח שכבר מחובר לשרת (connectBlocking() כבר
    // הצליח) - נקראת מ-HomeScreenMain אחרי שהיא מחברת GameClient משלה
    // (לפי room שהוזן במסך הבית), בלי לשכפל כאן את כל חיווט ה-Swing/Timer.
    // אין כאן main() עצמאי בכוונה - kfchess.LoginScreenMain הוא המיין
    // היחיד להרצת הלקוח (Login/Register → room → המסך הזה, בשרשרת אחת).
    public static void launch(GameClient client) {
        Gson gson = new Gson();
        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        SnapshotFactory snapshotFactory = new SnapshotFactory();

        BoardView boardView = new BoardView("src/main/resources/board.png");
        GameSceneView sceneView = new GameSceneView(boardView, SIDE_PANEL_WIDTH);
        // sceneView צריך להיבנות לפני clickHandler - הוא נחוץ ל-NetworkClickHandler
        // כדי לשאול restartButtonBounds() (ר' תיעוד שם) כשהמשחק נגמר.
        NetworkClickHandler clickHandler = new NetworkClickHandler(client, new BoardMapper(), sceneView);
        Img windowAnchor = new Img();

        // "מצב אחרון ידוע" - מתחיל ריק (אין עדיין נתונים מהשרת), ומתעדכן
        // בכל הודעת SNAPSHOT חדשה. מערך של איבר אחד - כדי שניתן יהיה
        // לשנות אותו מתוך למבדה (קליק, Timer).
        ClientSnapshotReconstructor.Reconstructed[] latest = { emptyReconstructedBeforeFirstSnapshot() };
        String[] lastProcessedMessage = { null };

        // רינדור ראשון - עוד אין frame, אז BoardLayoutCalculator.currentContentSize
        // נופלת חזרה לגודל התחלתי קבוע. זה גם מה שפותח את החלון בפועל (show()).
        renderFrame(snapshotFactory, sceneView, windowAnchor, latest);

        javax.swing.SwingUtilities.invokeLater(() -> {
            windowAnchor.onClick((pixelX, pixelY) ->
                    handleClick(clickHandler, windowAnchor, latest, pixelX, pixelY, false));
            windowAnchor.onRightClick((pixelX, pixelY) ->
                    handleClick(clickHandler, windowAnchor, latest, pixelX, pixelY, true));
        });

        Timer timer = new Timer(16, e -> {
            pollAndDecode(client, gson, reconstructor, lastProcessedMessage, latest);
            renderFrame(snapshotFactory, sceneView, windowAnchor, latest);
        });
        timer.start();
    }

    // "מצב ריק" להצגה לפני שהתקבלה אפילו הודעה אחת מהשרת - כדי שהחלון
    // ייפתח מיד עם ההתחברות, בלי לחכות ל-snapshot ראשון (שעלול לקחת
    // כמה מילישניות). PLACEHOLDER_BOARD_SIZE משמש רק לפריסה ההתחלתית -
    // מוחלף מיד במידות האמיתיות מהשרת ברגע שמגיעה הודעה.
    private static ClientSnapshotReconstructor.Reconstructed emptyReconstructedBeforeFirstSnapshot() {
        return new ClientSnapshotReconstructor.Reconstructed(
                Board.createDefault(PLACEHOLDER_BOARD_SIZE, PLACEHOLDER_BOARD_SIZE),
                List.of(), List.of(), List.of(), null, List.of(), false, null, 0L, Map.of(), Map.of(), false, null);
    }

    // נקרא בכל טיק של ה-Timer: קורא את ההודעה האחרונה שהתקבלה מ-GameClient
    // (ר' GameClient.latestMessage) ומעדכן את latest[0] רק אם זו הודעת
    // SNAPSHOT *חדשה* (לא ROLE_ASSIGNED/ERROR, ולא אותה הודעה שכבר עובדה) -
    // כדי לא לפענח/לשחזר שוב את אותו JSON 60 פעם בשנייה כשמגיעות רק ~30
    // הודעות SNAPSHOT בשנייה מהשרת.
    private static void pollAndDecode(GameClient client, Gson gson, ClientSnapshotReconstructor reconstructor,
                                       String[] lastProcessedMessage,
                                       ClientSnapshotReconstructor.Reconstructed[] latest) {
        String message = client.latestMessage();
        if (message == null || message.equals(lastProcessedMessage[0]) || !IncomingMessageSummary.isSnapshot(message)) {
            return;
        }
        lastProcessedMessage[0] = message;
        IncomingSnapshot incoming = gson.fromJson(message, IncomingSnapshot.class);
        latest[0] = reconstructor.reconstruct(incoming);
    }

    // מטפל בקליק (רגיל/ימני) על הלוח: מחשב את ה-BoardLayout הנוכחי (תלוי
    // בגודל החלון בפועל + מידות הלוח האחרונות שהתקבלו - לא ניתן להוציא
    // מכאן, כי שניהם תלויים במצב חי של Swing/latest[0]) ומעביר אותו ל-
    // NetworkClickHandler, שמטפל בהמרת פיקסל→מיקום ובשליחה לשרת (ר' תיעוד
    // המחלקה שם - שם גם נבדקת הלוגיקה הזו בפועל, בלי Swing/רשת אמיתיים).
    private static void handleClick(NetworkClickHandler clickHandler, Img windowAnchor,
                                     ClientSnapshotReconstructor.Reconstructed[] latest,
                                     int pixelX, int pixelY, boolean isJump) {
        Board board = latest[0].board();
        BoardLayout layout = BoardLayoutCalculator.computeLayout(
                BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE),
                board.width(), board.height(), SIDE_PANEL_WIDTH);
        clickHandler.handle(pixelX, pixelY, layout, latest[0].gameOver(), isJump);
    }

    // מרכיב GameSnapshot מהמצב האחרון הידוע (latest[0]) ומצייר אותו - נקרא
    // בכל טיק, גם כשלא הגיעה הודעה חדשה (כדי שהציור יגיב מיד לשינוי גודל
    // חלון). הבנייה מחדש של ה-layout בכל קריאה (ולא שימוש ב-layout ששמור
    // מרגע הפענוח) היא מה שמונע פיקסלים "תקועים" אם המשתמשת משנה גודל
    // חלון בין שתי הודעות SNAPSHOT.
    private static void renderFrame(SnapshotFactory snapshotFactory, GameSceneView sceneView, Img windowAnchor,
                                     ClientSnapshotReconstructor.Reconstructed[] latest) {
        ClientSnapshotReconstructor.Reconstructed state = latest[0];
        Board board = state.board();
        Dimension content = BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE);
        BoardLayout layout = BoardLayoutCalculator.computeLayout(content, board.width(), board.height(), SIDE_PANEL_WIDTH);

        GameSnapshot snapshot = snapshotFactory.createSnapshot(
                board, layout.cellSize(), layout.cellSize(), state.now(),
                state.selected(), state.gameOver(), state.winner(),
                state.motions(), state.jumps(), state.captureEffects(),
                state.legalMoves(), state.scores(), state.moveLog(),
                state.restartRequestedByViewer(), state.disconnectSecondsRemaining());

        sceneView.render(snapshot, content.width, content.height,
                layout.boardPixelSize(), layout.offsetX(), layout.offsetY());
    }
}
