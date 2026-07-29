package kfchess.view;

import kfchess.engine.snapshot.CaptureEffectSnapshot;
import kfchess.engine.snapshot.PlaySnapshot;
import kfchess.engine.snapshot.PieceSnapshot;
import kfchess.engine.snapshot.PieceVisualState;
import kfchess.model.Position;
import java.awt.*;

/** Draws the board and everything on it - pieces, rest timers, capture effects, selection and legal-move markers. */
public class BoardView {

    private static final Color SELECTION_COLOR = new Color(255, 235, 59); // yellow - the selected square
    private static final Color LEGAL_MOVE_COLOR = new Color(30, 200, 30, 170); // translucent green - reachable squares
    private static final Color REST_SAND_COLOR = new Color(255, 200, 0, 120); // "sandglass" - draining yellow overlay
    private static final Color CAPTURE_EFFECT_COLOR = new Color(220, 30, 30); // red - fading "X" where a piece was captured

    private final String boardImagePath;


    public BoardView(String boardImagePath) {
        this.boardImagePath = boardImagePath;
    }

    /** Draws the board onto a fresh canvas and returns it - PlaySceneView composes and shows the final image. */
    public Img render(PlaySnapshot snapshot, BoardGeometry geometry) {
        int boardWidthPx = geometry.getCellWidth() * geometry.getCols();
        int boardHeightPx = geometry.getCellHeight() * geometry.getRows();

        // Loaded at the current target size (not the file's own size) so it follows window resizes.
        Img canvas = new Img().readAsFreshCanvas(boardImagePath, boardWidthPx, boardHeightPx);

        drawLegalMoveMarkers(canvas, snapshot, geometry);

        for (PieceSnapshot piece : snapshot.pieces()) {
            drawPiece(canvas, piece, geometry);
            drawRestOverlayIfResting(canvas, piece, geometry);
        }

        // Drawn on top of the pieces, so the fading "X" stays visible even if another piece already occupies the square.
        for (CaptureEffectSnapshot effect : snapshot.captureEffects()) {
            drawCaptureEffect(canvas, effect, geometry);
        }

        drawSelectionHighlight(canvas, snapshot, geometry);

        return canvas;
    }

    /** Draws a fading red "X" and expanding ring where a piece was just captured (progress 0 = fresh, 1 = gone). */
    private void drawCaptureEffect(Img canvas, CaptureEffectSnapshot effect, BoardGeometry geometry) {
        double fadeOut = 1.0 - effect.progress();
        int alpha = (int) Math.round(220 * fadeOut);
        if (alpha <= 0) {
            return;
        }

        int cx = (int) Math.round(effect.pixelX() + geometry.getCellWidth() / 2.0);
        int cy = (int) Math.round(effect.pixelY() + geometry.getCellHeight() / 2.0);
        int baseSize = Math.min(geometry.getCellWidth(), geometry.getCellHeight());
        // The ring grows slightly as it fades, so it reads as "dispersing" rather than just disappearing.
        int ringSize = (int) Math.round(baseSize * (0.55 + 0.35 * effect.progress()));
        Color ringColor = new Color(CAPTURE_EFFECT_COLOR.getRed(), CAPTURE_EFFECT_COLOR.getGreen(),
                CAPTURE_EFFECT_COLOR.getBlue(), alpha);

        canvas.drawRect(cx - ringSize / 2, cy - ringSize / 2, ringSize, ringSize, ringColor, 5);

        String mark = "\u2715"; // ✕
        int markFontSize = (int) Math.round(baseSize * 0.5);
        int markWidth = canvas.textWidth(mark, markFontSize, true);
        canvas.drawText(mark, cx - markWidth / 2, cy + markFontSize / 3, markFontSize, ringColor, true);
    }

    /** Draws one piece's current animation frame, centered in its square. */
    private void drawPiece(Img canvas, PieceSnapshot piece, BoardGeometry geometry) {
        String framePath = currentFramePathFor(piece);
        Img pieceImg = new Img().read(
                framePath,
                new Dimension(geometry.getCellWidth(), geometry.getCellHeight()),
                true, null
        );
        // keepAspect=true means the scaled sprite is usually smaller than the cell on one axis.
        // Offsetting by half the difference centers it, instead of pinning it to the top-left corner.
        int cellX = (int) Math.round(piece.pixelX());
        int cellY = (int) Math.round(piece.pixelY());
        int x = cellX + (geometry.getCellWidth() - pieceImg.width()) / 2;
        int y = cellY + (geometry.getCellHeight() - pieceImg.height()) / 2;
        pieceImg.drawOn(canvas, x, y);
    }

    /** "Sandglass": a translucent overlay on a resting piece's square that drains as its cooldown elapses. */
    private void drawRestOverlayIfResting(Img canvas, PieceSnapshot piece, BoardGeometry geometry) {
        boolean resting = piece.state() == PieceVisualState.SHORT_REST
                || piece.state() == PieceVisualState.LONG_REST;
        if (!resting) {
            return;
        }
        double remainingFraction = 1.0 - piece.restProgress();
        int overlayHeight = (int) Math.round(geometry.getCellHeight() * remainingFraction);
        if (overlayHeight <= 0) {
            return;
        }
        int x = (int) Math.round(piece.pixelX());
        int y = (int) Math.round(piece.pixelY());
        canvas.fillRect(x, y, geometry.getCellWidth(), overlayHeight, REST_SAND_COLOR);
    }

    /** Outlines the viewer's currently selected square, if any. */
    private void drawSelectionHighlight(Img canvas, PlaySnapshot snapshot, BoardGeometry geometry) {
        if (snapshot.selectedPosition() == null) {
            return;
        }
        Point topLeft = geometry.cellToPixel(snapshot.selectedPosition());
        canvas.drawRect(topLeft.x, topLeft.y, geometry.getCellWidth(), geometry.getCellHeight(), SELECTION_COLOR, 4);
    }

    /** Draws a dot on every square the selected piece could legally move to. */
    private void drawLegalMoveMarkers(Img canvas, PlaySnapshot snapshot, BoardGeometry geometry) {
        for (Position target : snapshot.legalMoves()) {
            Point topLeft = geometry.cellToPixel(target);
            int markerSize = Math.min(geometry.getCellWidth(), geometry.getCellHeight()) / 3;
            int cx = topLeft.x + (geometry.getCellWidth() - markerSize) / 2;
            int cy = topLeft.y + (geometry.getCellHeight() - markerSize) / 2;
            canvas.fillOval(cx, cy, markerSize, markerSize, LEGAL_MOVE_COLOR);
        }
    }

    /** Resolves the sprite file for a piece's current visual state and elapsed animation time. */
    private String currentFramePathFor(PieceSnapshot piece) {
        String folder = "" + piece.kind().code() + Character.toUpperCase(piece.color().code());
        String stateFolder = stateFolderFor(piece.state());
        String spritesFolder = "src/main/resources/pieces/" + folder + "/states/" + stateFolder + "/sprites";

        AnimationClip clip = AnimationClipCache.get(
                spritesFolder, framesPerSecFor(piece.state()), isLoopFor(piece.state())
        );
        int frameIndex = clip.getFrameIndex(piece.stateElapsedMillis());
        return clip.getFramePath(frameIndex);
    }

    /** The sprite sub-folder name for each visual state. */
    private String stateFolderFor(PieceVisualState state) {
        return switch (state) {
            case IDLE -> "idle";
            case MOVING -> "move";
            case JUMPING -> "jump";
            case SHORT_REST -> "short_rest";
            case LONG_REST -> "long_rest";
        };
    }

    /** Playback speed per visual state - moving animates faster than idling. */
    private int framesPerSecFor(PieceVisualState state) {
        return switch (state) {
            case IDLE -> 6;
            case MOVING -> 12;
            case JUMPING, SHORT_REST -> 8;
            case LONG_REST -> 6;
        };
    }

    /** Every state loops except JUMPING, which plays once and holds its last frame. */
    private boolean isLoopFor(PieceVisualState state) {
        return state != PieceVisualState.JUMPING;
    }
}
