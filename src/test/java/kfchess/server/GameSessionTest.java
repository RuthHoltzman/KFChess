package kfchess.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kfchess.protocol.ClientCommand;
import kfchess.model.ClientRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את GameSession: הקצאת תפקידים, ניתוב פקודות מהתור אל GameCommandController
 * ב-tick, והתוכן של ה-snapshot שנבנה לכל צבע. ה-WebSocket משמש כאן רק
 * כמפתח-זהות (ר' FakeWebSocket) - אין חיבור רשת אמיתי בטסטים האלה.
 */
class GameSessionTest {

    // לוח מזערי - מלך שחור ב-(0,0), צריח לבן ב-(1,0), צעד אחד ישר ביניהם
    // (מהלך צריח חוקי לגמרי) - כדי שלכידת מלך תהיה מהלך אחד בודד בטסט,
    // בלי לשחק משחק שלם על לוח הפתיחה הסטנדרטי (32 כלים). ר' הבנאי
    // GameSession(String, AccountRepository) - הוזרק בדיוק בשביל זה.
    private static final String ONE_MOVE_FROM_CAPTURE_BOARD = """
            Board:
            bK .  .  .  .  .  .  .
            wR .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            """;

    private final Gson gson = new Gson();

    private ClientCommand click(int row, int col) {
        return gson.fromJson("{\"type\":\"CLICK\",\"row\":" + row + ",\"col\":" + col + "}", ClientCommand.class);
    }

    private ClientCommand restart() {
        return gson.fromJson("{\"type\":\"RESTART\"}", ClientCommand.class);
    }

    // בונה session על הלוח המזערי, מזיז את הצריח הלבן על המלך השחור, ומקדם
    // את השעון מספיק (1000ms/משבצת, ר' GameEngine.MILLISECONDS_PER_SQUARE)
    // כדי שהמהלך יסתיים בפועל ולכידת המלך תתרחש - אחרי הקריאה הזו
    // engine.isGameOver()==true בוודאות.
    private GameSession sessionAfterKingCapture(FakeWebSocket white, FakeWebSocket black) {
        GameSession session = new GameSession(ONE_MOVE_FROM_CAPTURE_BOARD, null);
        session.assignRole(white);
        session.assignRole(black);
        session.enqueueCommand(white, click(1, 0)); // בחירת הצריח הלבן
        session.enqueueCommand(white, click(0, 0)); // צעד אחד למעלה - לכידת המלך השחור
        session.tick(1500);
        return session;
    }

    @Test
    void assignRole_firstConnectionIsWhite_secondIsBlack_restAreSpectators() {
        GameSession session = new GameSession();

        assertEquals(ClientRole.WHITE, session.assignRole(new FakeWebSocket()));
        assertEquals(ClientRole.BLACK, session.assignRole(new FakeWebSocket()));
        assertEquals(ClientRole.SPECTATOR, session.assignRole(new FakeWebSocket()));
    }

    @Test
    void removeConnection_removesItFromConnectionsMap() {
        GameSession session = new GameSession();
        FakeWebSocket connection = new FakeWebSocket();
        session.assignRole(connection);

        session.removeConnection(connection);

        assertFalse(session.connections().containsKey(connection));
    }

    @Test
    void tick_clickOnOwnPiece_selectsItForThatColorOnly() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white);
        session.assignRole(black);

        session.enqueueCommand(white, click(6, 4)); // חייל לבן בשורת הפתיחה
        session.tick(0);

        JsonObject whiteView = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonObject selected = whiteView.getAsJsonObject("selected");
        assertEquals(6, selected.get("row").getAsInt());
        assertEquals(4, selected.get("col").getAsInt());

        JsonObject blackView = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertFalse(blackView.has("selected")); // הבחירה של הלבן לא "דולפת" לצבע אחר
    }

    @Test
    void tick_clickThenLegalTarget_movesPieceIntoTransit() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);
        session.assignRole(new FakeWebSocket()); // BLACK - בלי זה הקליק היה נחסם (ר' isWaitingForOpponent, בקשת רות)

        session.enqueueCommand(white, click(6, 4)); // בחירה
        session.enqueueCommand(white, click(5, 4)); // צעד אחד קדימה - חוקי
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonArray pieces = snapshot.getAsJsonArray("pieces");

        boolean foundInTransit = false;
        for (JsonElement element : pieces) {
            JsonObject pieceDto = element.getAsJsonObject();
            JsonObject position = pieceDto.getAsJsonObject("position");
            if (position.get("row").getAsInt() == 6 && position.get("col").getAsInt() == 4) {
                assertEquals("IN_TRANSIT", pieceDto.getAsJsonObject("piece").get("state").getAsString());
                foundInTransit = true;
            }
        }
        assertTrue(foundInTransit);
    }

    @Test
    void tick_clickThenLegalTarget_snapshotIncludesMatchingMotion() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);
        session.assignRole(new FakeWebSocket()); // BLACK - בלי זה הקליק היה נחסם (ר' isWaitingForOpponent, בקשת רות)

        session.enqueueCommand(white, click(6, 4));
        session.enqueueCommand(white, click(5, 4));
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonArray motions = snapshot.getAsJsonArray("motions");

        assertEquals(1, motions.size());
        JsonObject motion = motions.get(0).getAsJsonObject();
        assertEquals(6, motion.getAsJsonObject("from").get("row").getAsInt());
        assertEquals(5, motion.getAsJsonObject("to").get("row").getAsInt());
    }

    @Test
    void tick_spectatorCommand_isIgnored() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE
        session.assignRole(new FakeWebSocket()); // BLACK
        FakeWebSocket spectator = new FakeWebSocket();
        assertEquals(ClientRole.SPECTATOR, session.assignRole(spectator));

        session.enqueueCommand(spectator, click(6, 4));
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.SPECTATOR)).getAsJsonObject();
        assertFalse(snapshot.has("selected"));
    }

    @Test
    void tick_commandFromUnknownConnection_doesNotThrow() {
        GameSession session = new GameSession();
        FakeWebSocket unregistered = new FakeWebSocket(); // מעולם לא עבר assignRole

        session.enqueueCommand(unregistered, click(6, 4));

        assertDoesNotThrow(() -> session.tick(0));
    }

    @Test
    void snapshotFor_initialBoard_hasThirtyTwoPiecesAndZeroScores() {
        GameSession session = new GameSession();

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.SPECTATOR)).getAsJsonObject();

        assertEquals(32, snapshot.getAsJsonArray("pieces").size());
        assertFalse(snapshot.get("gameOver").getAsBoolean());
        assertFalse(snapshot.has("winner"));
        JsonObject scores = snapshot.getAsJsonObject("scores");
        assertEquals(0, scores.get("WHITE").getAsInt());
        assertEquals(0, scores.get("BLACK").getAsInt());
        assertEquals(0, snapshot.getAsJsonArray("motions").size());
        assertEquals(0, snapshot.getAsJsonArray("jumps").size());
        assertEquals(0, snapshot.getAsJsonArray("captureEffects").size());
    }

    @Test
    void restart_beforeGameOver_isIgnoredAndDoesNotRevertTheMove() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white);
        session.assignRole(black);

        // מהלך רגיל (חייל צעד אחד קדימה) - לא מסיים את המשחק בכלל.
        session.enqueueCommand(white, click(6, 4));
        session.enqueueCommand(white, click(5, 4));
        session.tick(1500);

        // שני הצדדים "מבקשים" RESTART בזמן שהמשחק עוד באמצע - אמור להתעלם לגמרי.
        session.enqueueCommand(white, restart());
        session.enqueueCommand(black, restart());
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertFalse(snapshot.get("gameOver").getAsBoolean());
        // אם ה-restart היה מתבצע בטעות, החייל היה חוזר ל-(6,4) - ולא נשאר ב-32 כלים באותם מיקומים.
        assertEquals(32, snapshot.getAsJsonArray("pieces").size());
    }

    @Test
    void restart_onlyOneSideRequests_doesNotResetBoard() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black);

        session.enqueueCommand(white, restart());
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean()); // עדיין נגמר - לא התאפס
        assertEquals(1, snapshot.getAsJsonArray("pieces").size()); // רק הצריח הלבן נשאר (המלך נלכד)
    }

    @Test
    void restart_bothSidesRequest_resetsBoardAndClearsGameOver() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black);

        session.enqueueCommand(white, restart());
        session.enqueueCommand(black, restart());
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertFalse(snapshot.get("gameOver").getAsBoolean());
        assertEquals(2, snapshot.getAsJsonArray("pieces").size()); // הלוח המזערי חזר למצבו המקורי (bK + wR)
    }

    @Test
    void restart_spectatorVoteDoesNotCountTowardsTheTwoSidesNeeded() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black);
        FakeWebSocket spectator = new FakeWebSocket();
        assertEquals(ClientRole.SPECTATOR, session.assignRole(spectator));

        // רק לבן + צופה מבקשים - שחור (הצד השני שבאמת נדרש) לא ביקש בכלל.
        session.enqueueCommand(white, restart());
        session.enqueueCommand(spectator, restart());
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean()); // עדיין לא התאפס
        assertEquals(1, snapshot.getAsJsonArray("pieces").size());
    }

    // --- שלב 5, חלק 1: ניתוק/auto-resign עם חלון חסד (ר' GameSession.processDisconnections/
    // resolveExpiredDisconnects/DISCONNECT_GRACE_MILLIS). כל הטסטים כאן משתמשים ב-tick()
    // כדי "לקפוץ" בזמן, בדיוק כמו הטסטים הקיימים - הדדליין נמדד לפי engine.now()
    // (שעון המשחק, מוזרק כ-RaelTime), לא שעון-קיר אמיתי, בדיוק בשביל זה.

    @Test
    void disconnect_duringActiveGame_opensGracePeriodInsteadOfImmediateLoss() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(white);
        session.tick(0); // מעבד את הניתוק (processDisconnections) - עדיין הרבה לפני 20 שניות

        assertFalse(session.connections().containsKey(white)); // החיבור המת כן הוסר...
        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertFalse(snapshot.get("gameOver").getAsBoolean()); // ...אבל היריב עדיין לא זכה - בתוך חלון החסד
    }

    @Test
    void disconnect_graceWindowExpires_opponentIsDeclaredWinner() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(white);
        session.tick(0); // פותח את חלון החסד (דדליין = engine.now() + 20000)
        session.tick(25_000); // מקדם את שעון המשחק מעבר לדדליין - חלון החסד פג

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean());
        assertEquals("BLACK", snapshot.get("winner").getAsString()); // מי שנשאר (שחור) ניצח, לא מי שהתנתק
    }

    @Test
    void disconnect_thenReconnectWithSameUsername_cancelsGraceAndRestoresRole() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(white);
        session.tick(0);

        FakeWebSocket whiteReconnected = new FakeWebSocket();
        ClientRole role = session.assignRole(whiteReconnected, "ruth");
        assertEquals(ClientRole.WHITE, role); // אותו תפקיד בדיוק בחזרה

        session.tick(30_000); // הרבה מעבר ל-20 שניות המקוריות - אבל חלון החסד כבר בוטל
        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertFalse(snapshot.get("gameOver").getAsBoolean());
    }

    @Test
    void disconnect_duringGrace_reservedRoleCannotBeTakenByUnrelatedConnection() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(white);
        session.tick(0);

        FakeWebSocket newcomer = new FakeWebSocket();
        ClientRole role = session.assignRole(newcomer, "someone-else");

        assertEquals(ClientRole.SPECTATOR, role); // WHITE עדיין שמור לרות, לא נגנב
    }

    @Test
    void disconnect_spectator_isRemovedImmediatelyWithoutGracePeriod() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE
        session.assignRole(new FakeWebSocket()); // BLACK
        FakeWebSocket spectator = new FakeWebSocket();
        session.assignRole(spectator);

        session.handleDisconnect(spectator);
        session.tick(0);

        assertFalse(session.connections().containsKey(spectator));
    }

    @Test
    void disconnect_afterGameAlreadyOver_isRemovedImmediatelyWithoutOpeningNewGracePeriod() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black); // המשחק כבר נגמר (לכידת מלך)

        session.handleDisconnect(black); // הצד שהפסיד מתנתק אחרי סוף המשחק
        session.tick(0);

        assertFalse(session.connections().containsKey(black));
        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean());
        assertEquals("WHITE", snapshot.get("winner").getAsString()); // עדיין המנצח המקורי, לא שונה בגלל הניתוק
    }

    // --- שלב 5, חלק 2: matchmaking (ר' GameSession.isWaitingForOpponent, נקראת
    // מ-GameServer.resolveMatchmakingGameId כדי לדעת אם לצרף שחקן/ית חדש/ה לכאן).

    @Test
    void isWaitingForOpponent_onlyWhiteConnected_returnsTrue() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE, אף אחד עדיין לא BLACK

        assertTrue(session.isWaitingForOpponent());
    }

    @Test
    void isWaitingForOpponent_noOneConnectedYet_returnsFalse() {
        GameSession session = new GameSession();

        assertFalse(session.isWaitingForOpponent()); // אין אף אחד לצרף אליו - לא "ממתין", פשוט ריק
    }

    @Test
    void isWaitingForOpponent_bothSidesConnected_returnsFalse() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE
        session.assignRole(new FakeWebSocket()); // BLACK

        assertFalse(session.isWaitingForOpponent());
    }

    @Test
    void isWaitingForOpponent_otherSideReservedByDisconnectGrace_returnsFalse() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(black); // שחור מתנתק באמצע משחק פעיל - נכנס לחלון חסד
        session.tick(0);

        // WHITE לבד מחובר עכשיו, אבל BLACK "שמור" ל-dani (לא באמת פנוי) -
        // matchmaking אסור לצרף כאן מישהו/י אקראי/ת במקום dani.
        assertFalse(session.isWaitingForOpponent());
    }

    @Test
    void isWaitingForOpponent_gameAlreadyOver_returnsFalse() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black);
        session.removeConnection(black); // רק לבן (המנצח) עדיין מחובר

        assertFalse(session.isWaitingForOpponent()); // המשחק נגמר - אין טעם לצרף אליו יריב/ה חדש/ה
    }

    // --- בקשת רות (הסבב הזה): כל עוד ממתינים ליריב (isWaitingForOpponent),
    // הצד היחיד שכבר מחובר לא יכול "להתחיל לשחק" - CLICK/JUMP מתעלמים
    // בשקט (ר' GameSession.applyCommand).

    @Test
    void tick_clickWhileWaitingForOpponent_isIgnored() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white); // רק WHITE מחובר - אין BLACK בכלל עדיין

        session.enqueueCommand(white, click(6, 4));
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertFalse(snapshot.has("selected")); // הקליק לא בוצע בכלל - עדיין ממתינים ליריב
    }

    @Test
    void tick_clickAfterOpponentJoins_isProcessedNormally() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);

        session.enqueueCommand(white, click(6, 4)); // לפני שהצטרף/ה יריב - יתעלם
        session.tick(0);

        session.assignRole(new FakeWebSocket()); // BLACK מצטרף עכשיו - אין יותר המתנה
        session.enqueueCommand(white, click(6, 4)); // אותו קליק בדיוק, הפעם אמור לעבוד
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonObject selected = snapshot.getAsJsonObject("selected");
        assertEquals(6, selected.get("row").getAsInt());
        assertEquals(4, selected.get("col").getAsInt());
    }

    // --- בקשת רות (הסבב הזה): שדה waitingForOpponent ב-snapshot עצמו
    // (בנוסף לחסימת הקליקים למעלה) - כדי שהלקוח יידע לצייר באנר "Waiting
    // for an opponent..." (ר' GameSceneView.drawWaitingForOpponentBanner).

    @Test
    void snapshotFor_onlyWhiteConnected_reportsWaitingForOpponentTrue() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // רק WHITE, אין BLACK

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertTrue(snapshot.get("waitingForOpponent").getAsBoolean());
    }

    @Test
    void snapshotFor_bothSidesConnected_reportsWaitingForOpponentFalse() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE
        session.assignRole(new FakeWebSocket()); // BLACK

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertFalse(snapshot.get("waitingForOpponent").getAsBoolean());
    }

    // --- תיקון "Play" לפי המפרט המדויק (ELO ±100 / timeout של דקה, הסבב
    // הזה): waitingPlayerUsername() - נחוץ ל-GameServer.resolveMatchmakingGameId
    // כדי לדעת של מי ה-ELO לבדוק מול המחפש/ת החדש/ה.

    @Test
    void waitingPlayerUsername_onlyOneSideConnectedWithUsername_returnsThatUsername() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket(), "ruth"); // רק WHITE, אין BLACK

        assertEquals(Optional.of("ruth"), session.waitingPlayerUsername());
    }

    @Test
    void waitingPlayerUsername_bothSidesConnected_returnsEmpty() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket(), "ruth");
        session.assignRole(new FakeWebSocket(), "dani");

        assertEquals(Optional.empty(), session.waitingPlayerUsername());
    }

    @Test
    void waitingPlayerUsername_noOneConnectedYet_returnsEmpty() {
        GameSession session = new GameSession();

        assertEquals(Optional.empty(), session.waitingPlayerUsername());
    }

    @Test
    void waitingPlayerUsername_waitingSideConnectedWithoutLogin_returnsEmpty() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // בלי username (חיבור אנונימי)

        assertEquals(Optional.empty(), session.waitingPlayerUsername());
    }
}
