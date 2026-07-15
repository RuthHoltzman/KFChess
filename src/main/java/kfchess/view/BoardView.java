package kfchess.view;

import kfchess.engine.GameSnapshot;
import kfchess.engine.PieceSnapshot;
import kfchess.engine.PieceVisualState;
import kfchess.model.Position;
import java.awt.*;

public class BoardView {

    private static final Color SELECTION_COLOR = new Color(255, 235, 59); // צהוב לבחירה
    private static final Color LEGAL_MOVE_COLOR = new Color(30, 200, 30, 170); // ירוק חצי-שקוף לתאים שאפשר לזוז אליהם
    private static final Color REST_SAND_COLOR = new Color(255, 200, 0, 120); // "שעון חול" - צהוב חצי-שקוף שמתרוקן

    private final String boardImagePath;
    private final BoardGeometry geometry;

    public BoardView(String boardImagePath, BoardGeometry geometry) {
        this.boardImagePath = boardImagePath;
        this.geometry = geometry;
    }

    public void render(GameSnapshot snapshot) {
        // קנבס טרי בכל render - אבל דרך readAsFreshCanvas, שמביא את
        // התמונה מקאש בזיכרון (ולא מהדיסק מחדש בכל פריים) ומחזיר לנו
        // עותק פרטי שמותר לצייר עליו בלי להשפיע על הפריים הבא.
        Img canvas = new Img().readAsFreshCanvas(boardImagePath);

        drawLegalMoveMarkers(canvas, snapshot);

        for (PieceSnapshot piece : snapshot.pieces()) {
            drawPiece(canvas, piece);
            drawRestOverlayIfResting(canvas, piece);
        }

        drawSelectionHighlight(canvas, snapshot);

        canvas.show();
    }

    private void drawPiece(Img canvas, PieceSnapshot piece) {
        String framePath = currentFramePathFor(piece);
        Img pieceImg = new Img().read(
                framePath,
                new Dimension(geometry.getCellWidth(), geometry.getCellHeight()),
                true, null
        );
        int x = (int) Math.round(piece.pixelX());
        int y = (int) Math.round(piece.pixelY());
        pieceImg.drawOn(canvas, x, y);
    }

    /**
     * "שעון חול": כל עוד הכלי במנוחה (קצרה אחרי הליכה, ארוכה אחרי קפיצה),
     * מציירים על המשבצת שלו מלבן צהוב חצי-שקוף שמתחיל מלא וגובהו הולך
     * ומצטמצם ככל שהמנוחה מתקדמת - עד שהוא נעלם לגמרי כשהמנוחה מסתיימת.
     */
    private void drawRestOverlayIfResting(Img canvas, PieceSnapshot piece) {
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

    private void drawSelectionHighlight(Img canvas, GameSnapshot snapshot) {
        if (snapshot.selectedPosition() == null) {
            return;
        }
        Point topLeft = geometry.cellToPixel(snapshot.selectedPosition());
        canvas.drawRect(topLeft.x, topLeft.y, geometry.getCellWidth(), geometry.getCellHeight(), SELECTION_COLOR, 4);
    }

    private void drawLegalMoveMarkers(Img canvas, GameSnapshot snapshot) {
        for (Position target : snapshot.legalMoves()) {
            Point topLeft = geometry.cellToPixel(target);
            int markerSize = Math.min(geometry.getCellWidth(), geometry.getCellHeight()) / 3;
            int cx = topLeft.x + (geometry.getCellWidth() - markerSize) / 2;
            int cy = topLeft.y + (geometry.getCellHeight() - markerSize) / 2;
            canvas.fillOval(cx, cy, markerSize, markerSize, LEGAL_MOVE_COLOR);
        }
    }

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

    private String stateFolderFor(PieceVisualState state) {
        return switch (state) {
            case IDLE -> "idle";
            case MOVING -> "move";
            case JUMPING -> "jump";
            case SHORT_REST -> "short_rest";
            case LONG_REST -> "long_rest";
        };
    }

    private int framesPerSecFor(PieceVisualState state) {
        return switch (state) {
            case IDLE -> 6;
            case MOVING -> 12;
            case JUMPING, SHORT_REST -> 8;
            case LONG_REST -> 6;
        };
    }

    private boolean isLoopFor(PieceVisualState state) {
        return state != PieceVisualState.JUMPING;
    }
}
