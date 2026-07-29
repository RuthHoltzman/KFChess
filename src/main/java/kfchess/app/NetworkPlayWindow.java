package kfchess.app;

import com.google.gson.Gson;
import kfchess.engine.snapshot.PlaySnapshot;
import kfchess.engine.snapshot.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.model.Board;
import kfchess.model.ClientRole;
import kfchess.client.ClientSnapshotReconstructor;
import kfchess.client.PlayClient;
import kfchess.client.IncomingMessageSummary;
import kfchess.client.IncomingSnapshot;
import kfchess.client.NetworkClickHandler;
import kfchess.view.BoardView;
import kfchess.view.PlaySceneView;
import kfchess.view.Img;
import kfchess.view.layout.BoardLayoutCalculator;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

/**
 * The game window itself, network-only: no local PlayEngine at all. PlayState
 * state always comes from the server, gets rebuilt into real domain
 * objects by ClientSnapshotReconstructor, and is painted through the same
 * SnapshotFactory/PlaySceneView the local game used to use. Clicks never
 * touch any engine - they only send CLICK/JUMP to the server, which is the
 * sole authority on whether a move is legal.
 */
public class NetworkPlayWindow {

    private static final int INITIAL_CELL_SIZE = 100;
    private static final int SIDE_PANEL_WIDTH = 240;
    // Placeholder board size, only so the window can open before the first
    // snapshot arrives - replaced immediately once a real message comes in.
    private static final int PLACEHOLDER_BOARD_SIZE = 8;

    /**
     * Opens the game window for a client that is already connected to the
     * server. Called from HomeScreen after it connects its own PlayClient.
     * Has no standalone main() on purpose - kfchess.app.LoginScreenMain is
     * the single entry point for running the client.
     */
    public static void launch(PlayClient client, String gameId, String username) {
        Img.setTitle("KFChess - Room: " + gameId);
        Gson gson = new Gson();
        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        SnapshotFactory snapshotFactory = new SnapshotFactory();

        ClientRole role = client.assignedRole();
        BoardView boardView = new BoardView("src/main/resources/board.png");
        PlaySceneView sceneView = new PlaySceneView(boardView, SIDE_PANEL_WIDTH, gameId, role, username);
        // sceneView must be built before clickHandler - NetworkClickHandler needs
        // it to ask restartButtonBounds() once the game is over.
        NetworkClickHandler clickHandler = new NetworkClickHandler(client, new BoardMapper(), sceneView);
        Img windowAnchor = new Img();

        // "Latest known state" - starts empty, updated on every new SNAPSHOT
        // message. Wrapped in a one-element array so lambdas (click, resize,
        // server message) can mutate it.
        ClientSnapshotReconstructor.Reconstructed[] latest = { emptyReconstructedBeforeFirstSnapshot() };

        // First render - also what actually opens the window (show()).
        renderFrame(snapshotFactory, sceneView, windowAnchor, latest);

        javax.swing.SwingUtilities.invokeLater(() -> {
            windowAnchor.onClick((pixelX, pixelY) ->
                    handleClick(clickHandler, windowAnchor, latest, pixelX, pixelY, false));
            windowAnchor.onRightClick((pixelX, pixelY) ->
                    handleClick(clickHandler, windowAnchor, latest, pixelX, pixelY, true));
            // Repaint immediately on window resize - the layout depends on the
            // live window size, not just on the game state.
            windowAnchor.onResize((width, height) -> renderFrame(snapshotFactory, sceneView, windowAnchor, latest));
        });

        // Runs on the network thread (PlayClient.onMessage) - hop onto the EDT
        // before touching Swing/latest[0]. Replaces the old fixed-rate Timer:
        // we now repaint only when the server actually sent something new.
        client.setMessageListener(message -> javax.swing.SwingUtilities.invokeLater(() ->
                handleServerMessage(message, client, gson, reconstructor, snapshotFactory, sceneView, windowAnchor, latest)));
    }

    /** Decodes a new snapshot and repaints, or handles a matchmaking timeout; always runs on the EDT. */
    private static void handleServerMessage(String message, PlayClient client, Gson gson,
                                             ClientSnapshotReconstructor reconstructor, SnapshotFactory snapshotFactory,
                                             PlaySceneView sceneView, Img windowAnchor,
                                             ClientSnapshotReconstructor.Reconstructed[] latest) {
        if (IncomingMessageSummary.isMatchmakingTimeout(message)) {
            handleMatchmakingTimeout(client.matchmakingTimeoutMessage());
            return;
        }
        if (!IncomingMessageSummary.isSnapshot(message)) {
            return;
        }
        IncomingSnapshot incoming = gson.fromJson(message, IncomingSnapshot.class);
        latest[0] = reconstructor.reconstruct(incoming);
        renderFrame(snapshotFactory, sceneView, windowAnchor, latest);
    }

    /** Shows a blocking popup when Play matchmaking times out, then exits the process. Must run on the EDT. */
    private static void handleMatchmakingTimeout(String message) {
        JOptionPane.showMessageDialog(null, message, "KFChess", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    /** Empty state shown before any server message has arrived, so the window opens immediately. */
    private static ClientSnapshotReconstructor.Reconstructed emptyReconstructedBeforeFirstSnapshot() {
        return new ClientSnapshotReconstructor.Reconstructed(
                Board.createDefault(PLACEHOLDER_BOARD_SIZE, PLACEHOLDER_BOARD_SIZE),
                List.of(), List.of(), List.of(), null, List.of(), false, null, 0L, Map.of(), Map.of(), false, null,
                false);
    }

    /** Routes a click/right-click on the board to NetworkClickHandler, with the current layout. */
    private static void handleClick(NetworkClickHandler clickHandler, Img windowAnchor,
                                     ClientSnapshotReconstructor.Reconstructed[] latest,
                                     int pixelX, int pixelY, boolean isJump) {
        Board board = latest[0].board();
        BoardLayout layout = computeBoardLayout(windowAnchor, board);
        clickHandler.handle(pixelX, pixelY, layout, latest[0].gameOver(), isJump);
    }

    /** Computes the board's on-screen layout, reserving space for the room header strip at the top. */
    private static BoardLayout computeBoardLayout(Img windowAnchor, Board board) {
        Dimension fullContent = BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE);
        int headerHeight = PlaySceneView.roomHeaderHeight();
        Dimension contentBelowHeader = new Dimension(fullContent.width, Math.max(1, fullContent.height - headerHeight));
        BoardLayout layout = BoardLayoutCalculator.computeLayout(
                contentBelowHeader, board.width(), board.height(), SIDE_PANEL_WIDTH);
        return new BoardLayout(layout.cellSize(), layout.boardPixelSize(), layout.offsetX(),
                layout.offsetY() + headerHeight);
    }

    /** Builds a PlaySnapshot from the latest known state and paints it; called on new data or a resize. */
    private static void renderFrame(SnapshotFactory snapshotFactory, PlaySceneView sceneView, Img windowAnchor,
                                     ClientSnapshotReconstructor.Reconstructed[] latest) {
        ClientSnapshotReconstructor.Reconstructed state = latest[0];
        Board board = state.board();
        Dimension content = BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE);
        BoardLayout layout = computeBoardLayout(windowAnchor, board);

        PlaySnapshot snapshot = snapshotFactory.createSnapshot(
                board, layout.cellSize(), layout.cellSize(), state.now(),
                state.selected(), state.gameOver(), state.winner(),
                state.motions(), state.jumps(), state.captureEffects(),
                state.legalMoves(), state.scores(), state.moveLog(),
                state.restartRequestedByViewer(), state.disconnectSecondsRemaining(),
                state.waitingForOpponent());

        sceneView.render(snapshot, content.width, content.height,
                layout.boardPixelSize(), layout.offsetX(), layout.offsetY());
    }
}
