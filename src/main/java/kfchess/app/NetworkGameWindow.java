package kfchess.app;

import com.google.gson.Gson;
import kfchess.engine.snapshot.GameSnapshot;
import kfchess.engine.snapshot.SnapshotFactory;
import kfchess.input.BoardMapper;
import kfchess.model.Board;
import kfchess.model.ClientRole;
import kfchess.client.ClientSnapshotReconstructor;
import kfchess.client.GameClient;
import kfchess.client.IncomingMessageSummary;
import kfchess.client.IncomingSnapshot;
import kfchess.client.NetworkClickHandler;
import kfchess.view.BoardView;
import kfchess.view.GameSceneView;
import kfchess.view.Img;
import kfchess.view.layout.BoardLayoutCalculator;
import kfchess.view.layout.BoardLayoutCalculator.BoardLayout;

import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

/**
 * The game window itself, network-only: no local GameEngine at all. Game
 * state always comes from the server, gets rebuilt into real domain
 * objects by ClientSnapshotReconstructor, and is painted through the same
 * SnapshotFactory/GameSceneView the local game used to use. Clicks never
 * touch any engine - they only send CLICK/JUMP to the server, which is the
 * sole authority on whether a move is legal.
 */
public class NetworkGameWindow {

    private static final int INITIAL_CELL_SIZE = 100;
    private static final int SIDE_PANEL_WIDTH = 240;
    // Placeholder board size, only so the window can open before the first
    // snapshot arrives - replaced immediately once a real message comes in.
    private static final int PLACEHOLDER_BOARD_SIZE = 8;

    /**
     * Opens the game window for a client that is already connected to the
     * server. Called from HomeScreen after it connects its own GameClient.
     * Has no standalone main() on purpose - kfchess.app.LoginScreenMain is
     * the single entry point for running the client.
     */
    public static void launch(GameClient client, String gameId, String username) {
        Img.setTitle("KFChess - Room: " + gameId);
        Gson gson = new Gson();
        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        SnapshotFactory snapshotFactory = new SnapshotFactory();

        ClientRole role = client.assignedRole();
        BoardView boardView = new BoardView("src/main/resources/board.png");
        GameSceneView sceneView = new GameSceneView(boardView, SIDE_PANEL_WIDTH, gameId, role, username);
        // sceneView must be built before clickHandler - NetworkClickHandler needs
        // it to ask restartButtonBounds() once the game is over.
        NetworkClickHandler clickHandler = new NetworkClickHandler(client, new BoardMapper(), sceneView);
        Img windowAnchor = new Img();

        // "Latest known state" - starts empty, updated on every new SNAPSHOT
        // message. Wrapped in a one-element array so lambdas (click, Timer) can mutate it.
        ClientSnapshotReconstructor.Reconstructed[] latest = { emptyReconstructedBeforeFirstSnapshot() };
        String[] lastProcessedMessage = { null };

        // First render - also what actually opens the window (show()).
        renderFrame(snapshotFactory, sceneView, windowAnchor, latest);

        javax.swing.SwingUtilities.invokeLater(() -> {
            windowAnchor.onClick((pixelX, pixelY) ->
                    handleClick(clickHandler, windowAnchor, latest, pixelX, pixelY, false));
            windowAnchor.onRightClick((pixelX, pixelY) ->
                    handleClick(clickHandler, windowAnchor, latest, pixelX, pixelY, true));
        });

        Timer[] timerRef = new Timer[1];
        Timer timer = new Timer(16, e -> {
            if (client.matchmakingTimeoutMessage() != null) {
                timerRef[0].stop();
                handleMatchmakingTimeout(client.matchmakingTimeoutMessage());
                return;
            }
            pollAndDecode(client, gson, reconstructor, lastProcessedMessage, latest);
            renderFrame(snapshotFactory, sceneView, windowAnchor, latest);
        });
        timerRef[0] = timer;
        timer.start();
    }

    /** Shows a blocking popup when Play matchmaking times out, then exits the process. */
    private static void handleMatchmakingTimeout(String message) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, "KFChess", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        });
    }

    /** Empty state shown before any server message has arrived, so the window opens immediately. */
    private static ClientSnapshotReconstructor.Reconstructed emptyReconstructedBeforeFirstSnapshot() {
        return new ClientSnapshotReconstructor.Reconstructed(
                Board.createDefault(PLACEHOLDER_BOARD_SIZE, PLACEHOLDER_BOARD_SIZE),
                List.of(), List.of(), List.of(), null, List.of(), false, null, 0L, Map.of(), Map.of(), false, null,
                false);
    }

    /** Reads the latest message from the server and decodes it, only if it's a new SNAPSHOT. */
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
        int headerHeight = GameSceneView.roomHeaderHeight();
        Dimension contentBelowHeader = new Dimension(fullContent.width, Math.max(1, fullContent.height - headerHeight));
        BoardLayout layout = BoardLayoutCalculator.computeLayout(
                contentBelowHeader, board.width(), board.height(), SIDE_PANEL_WIDTH);
        return new BoardLayout(layout.cellSize(), layout.boardPixelSize(), layout.offsetX(),
                layout.offsetY() + headerHeight);
    }

    /** Builds a GameSnapshot from the latest known state and paints it; runs every timer tick. */
    private static void renderFrame(SnapshotFactory snapshotFactory, GameSceneView sceneView, Img windowAnchor,
                                     ClientSnapshotReconstructor.Reconstructed[] latest) {
        ClientSnapshotReconstructor.Reconstructed state = latest[0];
        Board board = state.board();
        Dimension content = BoardLayoutCalculator.currentContentSize(windowAnchor, SIDE_PANEL_WIDTH, INITIAL_CELL_SIZE);
        BoardLayout layout = computeBoardLayout(windowAnchor, board);

        GameSnapshot snapshot = snapshotFactory.createSnapshot(
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
