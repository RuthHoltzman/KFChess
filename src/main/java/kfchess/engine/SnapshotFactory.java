package kfchess.engine;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.realtime.Motion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotFactory {

    // כמה גבוה (כשבר מגובה המשבצת) הכלי "עולה" בשיא הקפיצה - קבוע חזותי
    // טהור, לא קשור ללוגיקת המשחק. 0.35 נבחר כי זה מספיק בולט לעין בלי
    // שהכלי "יקפוץ" מחוץ למשבצת השכנה.
    private static final double JUMP_HEIGHT_FRACTION = 0.35;

    private final PieceVisualStateTracker visualStateTracker = new PieceVisualStateTracker();
    private final int cellWidth;
    private final int cellHeight;

    public SnapshotFactory(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public GameSnapshot createSnapshot(
            Board board,
            long now,
            Position selectedPosition,
            boolean gameOver,
            String winner,
            List<Motion> activeMotions,
            List<JumpVisual> activeJumps,
            List<CaptureEffect> captureEffects,
            List<Position> legalMoves,
            Map<PieceColor, Integer> scores,
            Map<PieceColor, List<String>> moveLog
    ) {
        // מיפוי כלי -> Motion פעיל, כדי לדעת עבור כל כלי אם הוא "בדרך"
        // כרגע ולחשב עבורו מיקום פיקסלים מתקדם (הליכה) ולא רק את המשבצת
        // המקורית (מה שהיה נראה כמו "קפיצה" ליעד ברגע שהמהלך הסתיים).
        Map<Piece, Motion> motionByPiece = new HashMap<>();
        for (Motion motion : activeMotions) {
            motionByPiece.put(motion.piece(), motion);
        }

        // מיפוי כלי -> קפיצה פעילה, כדי לחשב לכלים קופצים היסט אנכי
        // (קשת עלייה-ירידה) בנוסף לאנימציית הספרייטים - כך שגם אם הפריימים
        // עצמם דומים, רואים בבירור שהכלי "קפץ" ולא רק החליף פריים במקום.
        Map<Piece, JumpVisual> jumpByPiece = new HashMap<>();
        for (JumpVisual jump : activeJumps) {
            jumpByPiece.put(jump.piece(), jump);
        }

        List<PieceSnapshot> pieceSnapshots = new ArrayList<>();

        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Position pos = new Position(row, col);
                board.pieceAt(pos).ifPresent(piece -> {
                    PieceVisualState visualState = visualStateTracker.resolve(piece, now);
                    long stateElapsed = visualStateTracker.elapsedInCurrentVisualState(piece, now);
                    double restProgress = visualStateTracker.restProgress(piece, now);

                    double pixelX;
                    double pixelY;
                    Motion motion = motionByPiece.get(piece);
                    if (motion != null) {
                        double progress = motion.progress(now);
                        double fromX = motion.from().col() * cellWidth;
                        double fromY = motion.from().row() * cellHeight;
                        double toX = motion.to().col() * cellWidth;
                        double toY = motion.to().row() * cellHeight;
                        pixelX = fromX + (toX - fromX) * progress;
                        pixelY = fromY + (toY - fromY) * progress;
                    } else {
                        pixelX = pos.col() * cellWidth;
                        pixelY = pos.row() * cellHeight;

                        // כלי קופץ לא זז בין משבצות (הוא נשאר במקומו), ולכן
                        // לא עובר דרך ה-Motion branch למעלה - כאן מוסיפים לו
                        // היסט אנכי זמני לפי שבר ההתקדמות בתוך הקפיצה: עולה
                        // עד מחצית הזמן (progress=0.5) ויורד בחזרה עד הנחיתה.
                        JumpVisual jump = jumpByPiece.get(piece);
                        if (jump != null) {
                            double progress = progressBetween(jump.startTime(), jump.endTime(), now);
                            double arcHeight = cellHeight * JUMP_HEIGHT_FRACTION;
                            pixelY -= Math.sin(Math.PI * progress) * arcHeight;
                        }
                    }

                    String id = "" + piece.color().code() + piece.kind().code() + "@" + pos;

                    pieceSnapshots.add(new PieceSnapshot(
                            id, piece.kind(), piece.color(), visualState,
                            pixelX, pixelY, stateElapsed, restProgress
                    ));
                });
            }
        }

        List<CaptureEffectSnapshot> captureEffectSnapshots = new ArrayList<>();
        for (CaptureEffect effect : captureEffects) {
            double progress = progressBetween(
                    effect.removedAt(), effect.removedAt() + GameEngine.CAPTURE_EFFECT_DURATION_MS, now);
            if (progress >= 1.0) {
                continue; // כבר דהה לגמרי - GameEngine ינקה אותו בטיק הבא, אין מה לצייר
            }
            double px = effect.at().col() * cellWidth;
            double py = effect.at().row() * cellHeight;
            captureEffectSnapshots.add(new CaptureEffectSnapshot(effect.kind(), effect.color(), px, py, progress));
        }

        return new GameSnapshot(board.width(), board.height(), pieceSnapshots, captureEffectSnapshots,
                selectedPosition, legalMoves, gameOver, winner, scores, moveLog);
    }

    /** שבר התקדמות (0..1) בין start ל-end, לפי "עכשיו" נתון - זהה בעקרונו ל-Motion.progress. */
    private static double progressBetween(long start, long end, long now) {
        long duration = end - start;
        if (duration <= 0) {
            return 1.0;
        }
        double p = (now - start) / (double) duration;
        if (p < 0) return 0.0;
        if (p > 1) return 1.0;
        return p;
    }
}
