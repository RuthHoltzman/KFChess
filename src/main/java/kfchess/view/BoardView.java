package kfchess.view;

import kfchess.engine.CaptureEffectSnapshot;
import kfchess.engine.GameSnapshot;
import kfchess.engine.PieceSnapshot;
import kfchess.engine.PieceVisualState;
import kfchess.model.Position;
import java.awt.*;

public class BoardView {

    private static final Color SELECTION_COLOR = new Color(255, 235, 59); // צהוב לבחירה
    private static final Color LEGAL_MOVE_COLOR = new Color(30, 200, 30, 170); // ירוק חצי-שקוף לתאים שאפשר לזוז אליהם
    private static final Color REST_SAND_COLOR = new Color(255, 200, 0, 120); // "שעון חול" - צהוב חצי-שקוף שמתרוקן
    private static final Color CAPTURE_EFFECT_COLOR = new Color(220, 30, 30); // אדום - "X" דוהה במקום שכלי נלכד

    private final String boardImagePath;
    private final BoardGeometry geometry;

    public BoardView(String boardImagePath, BoardGeometry geometry) {
        this.boardImagePath = boardImagePath;
        this.geometry = geometry;
    }

    /**
     * מציירת את הלוח + הכלים על קנבס טרי ומחזירה אותו (בלי להציג אותו).
     * ההצגה בפועל (canvas.show()) היא באחריות שכבת קומפוזיציה מעל
     * (GameSceneView) - כי אנחנו רוצים לצייר קודם את פאנלי הניקוד/המהלכים
     * לצידי הלוח, ורק אז להציג את התמונה השלמה פעם אחת.
     */
    public Img render(GameSnapshot snapshot) {
        // קנבס טרי בכל render - אבל דרך readAsFreshCanvas, שמביא את
        // התמונה מקאש בזיכרון (ולא מהדיסק מחדש בכל פריים) ומחזיר לנו
        // עותק פרטי שמותר לצייר עליו בלי להשפיע על הפריים הבא.
        Img canvas = new Img().readAsFreshCanvas(boardImagePath);

        drawLegalMoveMarkers(canvas, snapshot);

        for (PieceSnapshot piece : snapshot.pieces()) {
            drawPiece(canvas, piece);
            drawRestOverlayIfResting(canvas, piece);
        }

        // מצוירים אחרי הכלים (מעל), כדי שה"X" הדוהה יהיה גלוי בבירור גם
        // אם כלי אחר כבר עומד/עובר על אותה משבצת ברגע זה.
        for (CaptureEffectSnapshot effect : snapshot.captureEffects()) {
            drawCaptureEffect(canvas, effect);
        }

        drawSelectionHighlight(canvas, snapshot);

        return canvas;
    }

    /**
     * מציירת "X" אדום דוהה + טבעת מתרחבת במקום שבו כלי נלכד הרגע (בין אם
     * זו לכידה רגילה, ובין אם זו "התאדות" של תוקף מול כלי קופץ) - כדי
     * שהלכידה תהיה ברורה לעין ולא תיראה כאילו "כלום לא קרה" בין פריים
     * לפריים. progress=0 זה הרגע שנתפס (הכי בולט), progress=1 זה הרגע
     * שבו האפקט אמור להיעלם לגמרי (הכי דהוי ומורחב).
     */
    private void drawCaptureEffect(Img canvas, CaptureEffectSnapshot effect) {
        double fadeOut = 1.0 - effect.progress();
        int alpha = (int) Math.round(220 * fadeOut);
        if (alpha <= 0) {
            return;
        }

        int cx = (int) Math.round(effect.pixelX() + geometry.getCellWidth() / 2.0);
        int cy = (int) Math.round(effect.pixelY() + geometry.getCellHeight() / 2.0);
        int baseSize = Math.min(geometry.getCellWidth(), geometry.getCellHeight());
        // הטבעת מתרחבת קצת תוך כדי שהיא דוהה - נותן תחושת "התפזרות" ולא רק היעלמות.
        int ringSize = (int) Math.round(baseSize * (0.55 + 0.35 * effect.progress()));
        Color ringColor = new Color(CAPTURE_EFFECT_COLOR.getRed(), CAPTURE_EFFECT_COLOR.getGreen(),
                CAPTURE_EFFECT_COLOR.getBlue(), alpha);

        canvas.drawRect(cx - ringSize / 2, cy - ringSize / 2, ringSize, ringSize, ringColor, 5);

        String mark = "\u2715"; // ✕
        int markFontSize = (int) Math.round(baseSize * 0.5);
        int markWidth = canvas.textWidth(mark, markFontSize, true);
        canvas.drawText(mark, cx - markWidth / 2, cy + markFontSize / 3, markFontSize, ringColor, true);
    }

    private void drawPiece(Img canvas, PieceSnapshot piece) {
        String framePath = currentFramePathFor(piece);
        Img pieceImg = new Img().read(
                framePath,
                new Dimension(geometry.getCellWidth(), geometry.getCellHeight()),
                true, null
        );
        // read(..., keepAspect=true) שומרת על יחס הגובה-רוחב של הספרייט, ולכן
        // הגודל בפועל של pieceImg כמעט תמיד קטן מ-cellWidth/cellHeight באחד
        // הצירים (למשל ספרייט "רזה" יותר מהמשבצת). אם מציירים אותו פשוט
        // בפינה השמאלית-עליונה של המשבצת (כמו קודם), הוא ייראה "דחוק"
        // לפינה במקום להיות ממורכז בה - זו בדיוק התופעה של "כלים לא
        // במיקום המדויק של הריבוע". כאן מחשבים את מרכז המשבצת ומזיזים
        // את פינת הציור כך שהספרייט (בגודלו האמיתי אחרי הסקייל) יתמרכז בה.
        int cellX = (int) Math.round(piece.pixelX());
        int cellY = (int) Math.round(piece.pixelY());
        int x = cellX + (geometry.getCellWidth() - pieceImg.width()) / 2;
        int y = cellY + (geometry.getCellHeight() - pieceImg.height()) / 2;
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
