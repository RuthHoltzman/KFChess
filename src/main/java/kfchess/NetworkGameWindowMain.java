package kfchess;

import com.google.gson.Gson;
import kfchess.engine.snapshot.GameSnapshot;
import kfchess.engine.snapshot.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.model.Board;
import kfchess.model.Position;
import kfchess.net.client.ClientSnapshotReconstructor;
import kfchess.net.client.GameClient;
import kfchess.net.client.IncomingMessageSummary;
import kfchess.net.client.IncomingSnapshot;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.Img;
import kfchess.view.layout.BoardLayoutCalculator;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import javax.swing.Timer;
import java.awt.Dimension;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * חלון Swing במצב רשת - מקביל ל-GameWindowMain, אבל בלי GameEngine
 * מקומי בכלל: כל מצב המשחק מגיע מהשרת (ר' GameClient), עובר שחזור
 * (ClientSnapshotReconstructor) חזרה לאובייקטי תחום אמיתיים, ואז מצויר
 * דרך אותו בדיוק SnapshotFactory/GameSceneView שהמשחק המקומי משתמש בהם -
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
    private static final String DEFAULT_SERVER_URI = "ws://localhost:8887/default";
    // גודל לוח זמני, רק כדי שהחלון יוכל להיפתח *לפני* שמתקבל snapshot
    // ראשון מהשרת - מוחלף מיד ברגע שמגיעה הודעה אמיתית (ר' boardWidthCells
    // /boardHeightCells שכבר מגיעים ברשת, לא hard-code בפועל).
    private static final int PLACEHOLDER_BOARD_SIZE = 8;

    // נקודת הכניסה: מתחברת לשרת (בדיוק כמו ClientMain), פותחת את החלון,
    // ואז מפעילה Timer של Swing שסוקר (polling) את ההודעה האחרונה מהשרת
    // ומצייר לפיה - ר' תיעוד המחלקה למעלה למה polling ולא callback.
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        String serverUri = args.length > 0 ? args[0] : DEFAULT_SERVER_URI;
        GameClient client = new GameClient(new URI(serverUri));
        if (!client.connectBlocking()) {
            System.err.println("failed to connect to " + serverUri);
            return;
        }

        Gson gson = new Gson();
        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        SnapshotFactory snapshotFactory = new SnapshotFactory();
        BoardMapper boardMapper = new BoardMapper();

        BoardView boardView = new BoardView("src/main/resources/board.png");
        GameSceneView sceneView = new GameSceneView(boardView, SIDE_PANEL_WIDTH);
        Img windowAnchor = new Img();

        // "מצב אחרון ידוע" - מתחיל ריק (אין עדיין נתונים מהשרת), ומתעדכן
        // בכל הודעת SNAPSHOT חדשה. מערך של איבר אחד, כמו session[0] ב-
        // GameWindowMain - כדי שניתן יהיה לשנות אותו מתוך למבדה (קליק, Timer).
        ClientSnapshotReconstructor.Reconstructed[] latest = { emptyReconstructedBeforeFirstSnapshot() };
        String[] lastProcessedMessage = { null };

        // רינדור ראשון - עוד אין frame, אז BoardLayoutCalculator.currentContentSize
        // נופלת חזרה לגודל התחלתי קבוע. זה גם מה שפותח את החלון בפועל (show()).
        renderFrame(snapshotFactory, sceneView, windowAnchor, latest);

        javax.swing.SwingUtilities.invokeLater(() -> {
            windowAnchor.onClick((pixelX, pixelY) ->
                    handleClick(client, boardMapper, windowAnchor, latest, pixelX, pixelY, false));
            windowAnchor.onRightClick((pixelX, pixelY) ->
                    handleClick(client, boardMapper, windowAnchor, latest, pixelX, pixelY, true));
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
                List.of(), List.of(), List.of(), null, List.of(), false, null, 0L, Map.of(), Map.of());
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

    // מטפל בקליק (רגיל/ימני) על הלוח: ממיר פיקסלים למיקום לוגי (BoardMapper,
    // אותה מחלקה שהמשחק המקומי משתמש בה) ושולח CLICK/JUMP לשרת. לא נוגע
    // במנוע בכלל - אין GameEngine מקומי במצב רשת (ר' תיעוד המחלקה למעלה).
    private static void handleClick(GameClient client, BoardMapper boardMapper, Img windowAnchor,
                                     ClientSnapshotReconstructor.Reconstructed[] latest,
                                     int pixelX, int pixelY, boolean isJump) {
        if (latest[0].gameOver()) {
            return; // אין עדיין כפתור Restart במצב רשת - ר' "הצעד הבא" ב-PROGRESS.md
        }
        Board board = latest[0].board();
        BoardLayout layout = BoardLayoutCalculator.computeLayout(
                BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE),
                board.width(), board.height(), SIDE_PANEL_WIDTH);
        int boardX = pixelX - layout.offsetX();
        int boardY = pixelY - layout.offsetY();
        if (boardX < 0 || boardX >= layout.boardPixelSize() || boardY < 0 || boardY >= layout.boardPixelSize()) {
            return; // קליק מחוץ ללוח - בפאנל, או בשוליים הריקים סביב הלוח הממורכז
        }
        Position clicked = boardMapper.pixelToPosition(boardX, boardY, layout.cellSize(), layout.cellSize());
        if (isJump) {
            client.sendJump(clicked.row(), clicked.col());
        } else {
            client.sendClick(clicked.row(), clicked.col());
        }
    }

    // מרכיב GameSnapshot מהמצב האחרון הידוע (latest[0]) ומצייר אותו - נקרא
    // בכל טיק, גם כשלא הגיעה הודעה חדשה (כדי שהציור יגיב מיד לשינוי גודל
    // חלון), בדיוק כמו renderFrame ב-GameWindowMain המקומי. הבנייה מחדש של
    // ה-layout בכל קריאה (ולא שימוש ב-layout ששמור מרגע הפענוח) היא מה
    // שמונע פיקסלים "תקועים" אם המשתמשת משנה גודל חלון בין שתי הודעות SNAPSHOT.
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
                state.legalMoves(), state.scores(), state.moveLog());

        sceneView.render(snapshot, content.width, content.height,
                layout.boardPixelSize(), layout.offsetX(), layout.offsetY());
    }
}
