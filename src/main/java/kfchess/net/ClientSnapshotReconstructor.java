package kfchess.net;

import kfchess.engine.snapshot.CaptureEffect;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceState;
import kfchess.model.Position;
import kfchess.realtime.Motion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * הופך IncomingSnapshot (JSON מפוענח, בלי זהות אובייקטים בין הודעות)
 * ל-Board+Motion+JumpVisual "אמיתיים" - עם אותם אובייקטי Piece בדיוק בין
 * הודעות עוקבות, לפי piece.id() (ר' kfchess.model.Piece) - כדי ש-
 * SnapshotFactory הקיים (הזהה למשחק המקומי) יעבוד בלי שינוי, כולל
 * אנימציית שעון-החול (SHORT_REST/LONG_REST) שתלויה בזיהוי "זה אותו כלי
 * שהיה קודם".
 * <p>
 * יש כאן state מכוון (knownPieces, לא מחלקה טהורה כמו GameIdResolver) -
 * חובה לזכור בין הודעות כדי לממש שימור זהות.
 */
public class ClientSnapshotReconstructor {

    private final Map<Long, Piece> knownPieces = new HashMap<>();

    /** תוצאת השחזור - בדיוק מה ש-SnapshotFactory.createSnapshot(...) צריך כקלט. */
    public record Reconstructed(
            Board board,
            List<Motion> motions,
            List<JumpVisual> jumps,
            List<CaptureEffect> captureEffects,
            Position selected,
            List<Position> legalMoves,
            boolean gameOver,
            String winner,
            long now,
            Map<PieceColor, Integer> scores,
            Map<PieceColor, List<String>> moveLog
    ) {}

    public Reconstructed reconstruct(IncomingSnapshot incoming) {
        Board board = Board.createDefault(incoming.boardHeightCells(), incoming.boardWidthCells());

        // שלב 1: לפתור/לסנכרן את כל הכלים לפי הרשימה השטוחה (pieces) - זה
        // המקור הסמכותי למצב (state) של כל כלי - ולמקם אותם על הלוח.
        for (PieceDto dto : incoming.pieces()) {
            Piece resolved = resolvePiece(dto.piece());
            board.placePiece(dto.position(), resolved);
        }

        // שלב 2: motions/jumps מגיעים עם עותקי Piece נפרדים (Gson יצר אובייקט
        // חדש לכל אחד) - resolvePiece עם אותו id מחזירה את האובייקט שכבר
        // הונח על הלוח בשלב 1, כך שהזהות תואמת בדיוק (נחוץ ל-HashMap lookup
        // בתוך SnapshotFactory).
        List<Motion> motions = new ArrayList<>();
        for (Motion motion : incoming.motions()) {
            Piece resolved = resolvePiece(motion.piece());
            motions.add(new Motion(resolved, motion.from(), motion.to(), motion.startTime(), motion.arrivalTime()));
        }

        List<JumpVisual> jumps = new ArrayList<>();
        for (JumpDto dto : incoming.jumps()) {
            Piece resolved = resolvePiece(dto.jump().piece());
            jumps.add(new JumpVisual(resolved, dto.jump().startTime(), dto.jump().endTime()));
        }

        return new Reconstructed(
                board, motions, jumps, incoming.captureEffects(),
                incoming.selected(), incoming.legalMoves(),
                incoming.gameOver(), incoming.winner(), incoming.now(),
                scoresByColor(incoming.scores()), moveLogByColor(incoming.moveLog()));
    }

    // לפי piece.id(): אם מוכר - מסנכרנת את המצב שלו ומחזירה את אותו אובייקט
    // (שימור זהות בין הודעות); אם לא - רושמת את האובייקט שהגיע (הוא כבר
    // אובייקט Piece אמיתי, אין צורך להעתיק) כ"מוכר" מעכשיו.
    private Piece resolvePiece(Piece incoming) {
        Piece known = knownPieces.get(incoming.id());
        if (known == null) {
            knownPieces.put(incoming.id(), incoming);
            return incoming;
        }
        syncState(known, incoming.state());
        return known;
    }

    // מיישרת את מצב האובייקט הקיים למצב הרצוי, רק דרך המתודות הציבוריות
    // של Piece עצמו - לא "דוחפת" ערך לשדה state ישירות, כדי לא לעקוף את
    // ההגנה מפני מעברי מצב לא חוקיים שכבר יש ל-Piece.
    private void syncState(Piece piece, PieceState desired) {
        if (piece.state() == desired) {
            return;
        }
        switch (desired) {
            case IN_TRANSIT -> piece.markInTransit();
            case JUMPING -> piece.markJumping();
            case IDLE -> {
                if (piece.isInTransit()) {
                    piece.markArrived();
                } else if (piece.isJumping()) {
                    piece.markJumpEnded();
                }
            }
        }
    }

    private Map<PieceColor, Integer> scoresByColor(Map<String, Integer> byName) {
        Map<PieceColor, Integer> byColor = new HashMap<>();
        byName.forEach((name, score) -> byColor.put(PieceColor.valueOf(name), score));
        return byColor;
    }

    private Map<PieceColor, List<String>> moveLogByColor(Map<String, List<String>> byName) {
        Map<PieceColor, List<String>> byColor = new HashMap<>();
        byName.forEach((name, log) -> byColor.put(PieceColor.valueOf(name), log));
        return byColor;
    }
}
