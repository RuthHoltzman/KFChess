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
 * Covers GameSession: role assignment, routing queued commands to GameCommandController on tick,
 * and the contents of the snapshot built for each color. The WebSocket is only an identity key
 * here (see FakeWebSocket) - these tests open no real network connection.
 */
class GameSessionTest {

    // Minimal board - black king at (0,0), white rook at (1,0), one straight step apart (a
    // perfectly legal rook move) - so capturing the king takes a single move instead of playing
    // a full 32-piece game. This is exactly what the injectable-board constructor is for.
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

    // Builds a session on the minimal board, moves the white rook onto the black king, and
    // advances the clock far enough (1000ms per square) for the move to actually complete.
    // After this call the game is guaranteed to be over.
    private GameSession sessionAfterKingCapture(FakeWebSocket white, FakeWebSocket black) {
        GameSession session = new GameSession(ONE_MOVE_FROM_CAPTURE_BOARD, null);
        session.assignRole(white);
        session.assignRole(black);
        session.enqueueCommand(white, click(1, 0)); // select the white rook
        session.enqueueCommand(white, click(0, 0)); // one step up - captures the black king
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

        session.enqueueCommand(white, click(6, 4)); // a white pawn on its starting rank
        session.tick(0);

        JsonObject whiteView = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonObject selected = whiteView.getAsJsonObject("selected");
        assertEquals(6, selected.get("row").getAsInt());
        assertEquals(4, selected.get("col").getAsInt());

        JsonObject blackView = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertFalse(blackView.has("selected")); // white's selection must not leak to the other color
    }

    @Test
    void tick_clickThenLegalTarget_movesPieceIntoTransit() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);
        session.assignRole(new FakeWebSocket()); // BLACK - without this the click would be blocked (isWaitingForOpponent)

        session.enqueueCommand(white, click(6, 4)); // select
        session.enqueueCommand(white, click(5, 4)); // one step forward - legal
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
        session.assignRole(new FakeWebSocket()); // BLACK - without this the click would be blocked (isWaitingForOpponent)

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
        FakeWebSocket unregistered = new FakeWebSocket(); // never went through assignRole

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

        // An ordinary move (pawn one step forward) - does not end the game.
        session.enqueueCommand(white, click(6, 4));
        session.enqueueCommand(white, click(5, 4));
        session.tick(1500);

        // Both sides ask for RESTART while the game is still running - must be ignored entirely.
        session.enqueueCommand(white, restart());
        session.enqueueCommand(black, restart());
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertFalse(snapshot.get("gameOver").getAsBoolean());
        // Had the restart wrongly gone through, the pawn would be back at (6,4).
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
        assertTrue(snapshot.get("gameOver").getAsBoolean()); // still over - not reset
        assertEquals(1, snapshot.getAsJsonArray("pieces").size()); // only the white rook is left
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
        assertEquals(2, snapshot.getAsJsonArray("pieces").size()); // minimal board restored (bK + wR)
    }

    @Test
    void restart_spectatorVoteDoesNotCountTowardsTheTwoSidesNeeded() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black);
        FakeWebSocket spectator = new FakeWebSocket();
        assertEquals(ClientRole.SPECTATOR, session.assignRole(spectator));

        // Only white and a spectator ask - black, the side that actually matters, never did.
        session.enqueueCommand(white, restart());
        session.enqueueCommand(spectator, restart());
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean()); // still not reset
        assertEquals(1, snapshot.getAsJsonArray("pieces").size());
    }

    // --- Disconnect / auto-resign with a grace window (see GameSession.processDisconnections,
    // resolveExpiredDisconnects, DISCONNECT_GRACE_MILLIS). These tests use tick() to jump forward
    // in time: the deadline is measured on the injected game clock, not on real wall-clock time,
    // which is precisely why it's injectable.

    @Test
    void disconnect_duringActiveGame_opensGracePeriodInsteadOfImmediateLoss() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(white);
        session.tick(0); // processes the disconnect - still well before 40 seconds

        assertFalse(session.connections().containsKey(white)); // the dead connection is removed...
        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertFalse(snapshot.get("gameOver").getAsBoolean()); // ...but the opponent has not won yet
    }

    @Test
    void disconnect_graceWindowExpires_opponentIsDeclaredWinner() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        session.assignRole(white, "ruth");
        session.assignRole(black, "dani");

        session.handleDisconnect(white);
        session.tick(0); // opens the grace window (deadline = now + DISCONNECT_GRACE_MILLIS)
        session.tick(45_000); // advances past the 40-second deadline - the grace window expires

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.BLACK)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean());
        assertEquals("BLACK", snapshot.get("winner").getAsString()); // whoever stayed wins, not the leaver
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
        assertEquals(ClientRole.WHITE, role); // exactly the same role back

        session.tick(45_000); // well past 40 seconds - but the reconnect already cancelled the window
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

        assertEquals(ClientRole.SPECTATOR, role); // WHITE is still reserved, not stolen
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
        GameSession session = sessionAfterKingCapture(white, black); // game already over (king captured)

        session.handleDisconnect(black); // the losing side leaves after the game ended
        session.tick(0);

        assertFalse(session.connections().containsKey(black));
        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertTrue(snapshot.get("gameOver").getAsBoolean());
        assertEquals("WHITE", snapshot.get("winner").getAsString()); // still the original winner
    }

    // --- Matchmaking: isWaitingForOpponent is called from GameServer.resolveMatchmakingGameId
    // to decide whether a new player can be placed into this session.

    @Test
    void isWaitingForOpponent_onlyWhiteConnected_returnsTrue() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE; nobody is BLACK yet

        assertTrue(session.isWaitingForOpponent());
    }

    @Test
    void isWaitingForOpponent_noOneConnectedYet_returnsFalse() {
        GameSession session = new GameSession();

        assertFalse(session.isWaitingForOpponent()); // nobody to join - not waiting, just empty
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

        session.handleDisconnect(black); // black drops mid-game - enters the grace window
        session.tick(0);

        // Only WHITE is connected now, but BLACK is reserved for dani and is not really free -
        // matchmaking must not drop a random player into dani's seat.
        assertFalse(session.isWaitingForOpponent());
    }

    @Test
    void isWaitingForOpponent_gameAlreadyOver_returnsFalse() {
        FakeWebSocket white = new FakeWebSocket();
        FakeWebSocket black = new FakeWebSocket();
        GameSession session = sessionAfterKingCapture(white, black);
        session.removeConnection(black); // only white, the winner, is still connected

        assertFalse(session.isWaitingForOpponent()); // game is over - no point adding an opponent
    }

    // --- While waiting for an opponent, the single connected side cannot start playing:
    // CLICK/JUMP are silently ignored (see GameSession.applyCommand).

    @Test
    void tick_clickWhileWaitingForOpponent_isIgnored() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white); // only WHITE is connected - there is no BLACK yet

        session.enqueueCommand(white, click(6, 4));
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        assertFalse(snapshot.has("selected")); // the click never applied - still waiting for an opponent
    }

    @Test
    void tick_clickAfterOpponentJoins_isProcessedNormally() {
        GameSession session = new GameSession();
        FakeWebSocket white = new FakeWebSocket();
        session.assignRole(white);

        session.enqueueCommand(white, click(6, 4)); // before an opponent joined - ignored
        session.tick(0);

        session.assignRole(new FakeWebSocket()); // BLACK joins now - no longer waiting
        session.enqueueCommand(white, click(6, 4)); // the exact same click, now it should work
        session.tick(0);

        JsonObject snapshot = gson.toJsonTree(session.snapshotFor(ClientRole.WHITE)).getAsJsonObject();
        JsonObject selected = snapshot.getAsJsonObject("selected");
        assertEquals(6, selected.get("row").getAsInt());
        assertEquals(4, selected.get("col").getAsInt());
    }

    // --- The waitingForOpponent field in the snapshot itself (on top of blocking clicks above),
    // so the client knows to draw the "Waiting for an opponent..." banner.

    @Test
    void snapshotFor_onlyWhiteConnected_reportsWaitingForOpponentTrue() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket()); // WHITE only, no BLACK

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

    // --- waitingPlayerUsername() is needed by GameServer.resolveMatchmakingGameId to know
    // whose ELO to compare against a new player searching for a match.

    @Test
    void waitingPlayerUsername_onlyOneSideConnectedWithUsername_returnsThatUsername() {
        GameSession session = new GameSession();
        session.assignRole(new FakeWebSocket(), "ruth"); // WHITE only, no BLACK

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
        session.assignRole(new FakeWebSocket()); // no username (anonymous connection)

        assertEquals(Optional.empty(), session.waitingPlayerUsername());
    }
}
