package kfchess.view;

import kfchess.engine.snapshot.GameSnapshot;
import kfchess.model.PieceColor;
import kfchess.model.ClientRole;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

/**
 * Top-level view: composes one canvas - White's panel | the board | Black's panel - and is the only class that calls show().
 * <p>
 * It computes no geometry itself; all sizes and offsets arrive as parameters from NetworkGameWindow,
 * so the same number is never calculated in two places that could drift apart.
 */
public class GameSceneView {

    private static final Color OUTER_BACKGROUND = new Color(30, 30, 30);
    private static final Color OVERLAY_BACKGROUND = new Color(0, 0, 0, 150);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color BUTTON_COLOR = new Color(46, 139, 87);
    private static final Color BUTTON_BORDER_COLOR = Color.WHITE;
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;
    // Warm red - reads as a warning, distinct from the black game-over overlay.
    private static final Color DISCONNECT_BANNER_BACKGROUND = new Color(120, 40, 20, 210);
    // Calm blue - neutral information ("not started yet"), deliberately not a warning color.
    private static final Color WAITING_BANNER_BACKGROUND = new Color(20, 60, 110, 210);
    // Neutral dark brown for the always-on room header, distinct from both state-dependent banners.
    private static final Color ROOM_HEADER_BACKGROUND = new Color(38, 34, 28);
    // Muted gold for the room id itself, so the eye separates the code from the word "Room".
    private static final Color ROOM_ID_ACCENT = new Color(201, 168, 118);
    // Dim tone for secondary header details (currently just the spectator label).
    private static final Color ROOM_HEADER_MUTED = new Color(154, 149, 135);

    private static final int TITLE_FONT_SIZE = 42;
    private static final int SUBTITLE_FONT_SIZE = 20;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 56;
    private static final int BUTTON_FONT_SIZE = 22;
    // Shared by both state-dependent banners (disconnect / waiting) - same geometry, different color and text.
    // These are drawn on the board canvas only, unlike ROOM_HEADER_HEIGHT below which spans the whole scene.
    private static final int TOP_BANNER_HEIGHT = 40;
    private static final int TOP_BANNER_FONT_SIZE = 20;
    // Height of the always-on room header. NetworkGameWindow must use this exact number
    // (via roomHeaderHeight()) when it shrinks the space left for the board.
    private static final int ROOM_HEADER_HEIGHT = 34;
    private static final int ROOM_HEADER_FONT_SIZE = 18;

    private final BoardView boardView;
    private final SidePanelView sidePanelView;
    // Fixed for the window's whole lifetime (set once at connect time), unlike everything else
    // that arrives per-frame in the GameSnapshot - hence constructor fields, not render() parameters.
    private final String roomId;
    private final ClientRole role;
    private final String username;

    // Refreshed at the start of every render(), so restartButtonBounds() (which takes no
    // parameters and is also called from click handling) knows what to measure against.
    private int lastBoardPixelSize;

    /** Convenience overload without room/role/username - used only by NetworkClickHandlerTest. */
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

    /** Pixels to reserve for the room header before laying out the board - the one constant outside code must know. */
    public static int roomHeaderHeight() {
        return ROOM_HEADER_HEIGHT;
    }

    /**
     * Restart button bounds, relative to the board's own top-left (not the scene's) - valid after at least one render().
     * Callers must subtract the board offset before testing a click against it.
     */
    public Rectangle restartButtonBounds() {
        int x = (lastBoardPixelSize - BUTTON_WIDTH) / 2;
        int y = lastBoardPixelSize / 2 + 30;
        return new Rectangle(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    /**
     * Composes and shows one full frame: room header, board (with any overlay/banner), and both side panels.
     *
     * @param sceneWidthPx   total drawable window width, including both panels.
     * @param sceneHeightPx  total drawable window height.
     * @param boardPixelSize board size in pixels - always square.
     * @param boardOffsetX   where the board starts on X (already accounts for the left panel and centering).
     * @param boardOffsetY   where the board starts on Y (vertical centering, if there's spare room).
     */
    public void render(GameSnapshot snapshot, int sceneWidthPx, int sceneHeightPx,
                        int boardPixelSize, int boardOffsetX, int boardOffsetY) {
        this.lastBoardPixelSize = boardPixelSize;

        BoardGeometry geometry = new BoardGeometry(
                boardPixelSize, boardPixelSize, snapshot.boardHeightCells(), snapshot.boardWidthCells());

        Img scene = new Img().newCanvas(sceneWidthPx, sceneHeightPx, OUTER_BACKGROUND);
        // Always drawn first, spanning the whole scene - it's game-wide info, not board-specific.
        // The boardOffsetY passed in already accounts for this header's height.
        drawRoomHeader(scene, sceneWidthPx);

        Img boardCanvas = boardView.render(snapshot, geometry);
        if (snapshot.gameOver()) {
            drawGameOverOverlay(boardCanvas, snapshot.winner(), snapshot.restartRequestedByViewer());
        }
        // In practice this never coincides with gameOver, but the conditions stay independent
        // on purpose - each reacts to what the snapshot actually contains, not to an assumption.
        if (snapshot.disconnectSecondsRemaining() != null) {
            drawDisconnectBanner(boardCanvas, snapshot.disconnectSecondsRemaining());
        }
        // Likewise mutually exclusive with the disconnect banner in practice, but kept independent.
        if (snapshot.waitingForOpponent()) {
            drawWaitingForOpponentBanner(boardCanvas);
        }
        boardCanvas.drawOn(scene, boardOffsetX, boardOffsetY);

        // Both panels start below the room header. isLocalPlayer marks whichever panel matches
        // this client's own role - a spectator owns neither, so neither gets the "You" badge.
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
     * Draws the game-over overlay: dark scrim, winner title, and a Restart button.
     * If this viewer already voted to restart, it shows "Waiting for opponent..." instead, so they get feedback.
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
     * Thin warning strip across the top of the board: "Opponent disconnected - Xs to reconnect".
     * Deliberately not a full overlay - the game isn't over, so the board stays visible; this only explains why it's frozen.
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
     * Same strip geometry as the disconnect banner, in neutral blue: "Waiting for an opponent to join...".
     * Shown while only one player is connected, so they understand why their clicks do nothing.
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
     * Draws the always-on header strip across the whole scene, so the room id is visible on screen and not just in the title bar.
     * WHITE/BLACK see their name in their own side panel instead; only a spectator gets their label here.
     */
    private void drawRoomHeader(Img scene, int sceneWidthPx) {
        scene.fillRect(0, 0, sceneWidthPx, ROOM_HEADER_HEIGHT, ROOM_HEADER_BACKGROUND);

        // Three segments, each its own color. Img.drawText can't mix colors in one string,
        // so each is measured separately to center the group, then drawn with an advancing X cursor.
        String label = "Room ";
        String id = shortRoomId(roomId);
        String spectatorSuffix = role == ClientRole.SPECTATOR
                ? "   Spectator" + (username != null ? ": " + username : "")
                : "";

        int labelWidth = scene.textWidth(label, ROOM_HEADER_FONT_SIZE, true);
        int idWidth = scene.textWidth(id, ROOM_HEADER_FONT_SIZE, true);
        int suffixWidth = spectatorSuffix.isEmpty() ? 0 : scene.textWidth(spectatorSuffix, ROOM_HEADER_FONT_SIZE, false);
        int totalWidth = labelWidth + idWidth + suffixWidth;

        int textY = ROOM_HEADER_HEIGHT / 2 + ROOM_HEADER_FONT_SIZE / 3;
        int cursorX = (sceneWidthPx - totalWidth) / 2;

        scene.drawText(label, cursorX, textY, ROOM_HEADER_FONT_SIZE, TITLE_COLOR, true);
        cursorX += labelWidth;
        scene.drawText(id, cursorX, textY, ROOM_HEADER_FONT_SIZE, ROOM_ID_ACCENT, true);
        cursorX += idWidth;
        if (!spectatorSuffix.isEmpty()) {
            scene.drawText(spectatorSuffix, cursorX, textY, ROOM_HEADER_FONT_SIZE, ROOM_HEADER_MUTED, false);
        }
    }

    /**
     * Shortens long matchmaking ids ("match-&lt;uuid&gt;") to the first 8 UUID chars for display.
     * Create/Join room codes are left untouched - the other player has to read and type them exactly.
     */
    public static String shortRoomId(String gameId) {
        if (gameId == null) {
            return "?";
        }
        String matchmakingPrefix = "match-";
        if (gameId.startsWith(matchmakingPrefix)) {
            String uuidPart = gameId.substring(matchmakingPrefix.length());
            return uuidPart.length() > 8 ? uuidPart.substring(0, 8) : uuidPart;
        }
        return gameId;
    }
}
