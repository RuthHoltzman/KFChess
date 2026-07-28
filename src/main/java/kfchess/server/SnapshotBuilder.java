package kfchess.server;

import kfchess.engine.GameCommandController;
import kfchess.engine.GameEngine;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Board;
import kfchess.model.ClientRole;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.protocol.JumpDto;
import kfchess.protocol.PieceDto;
import kfchess.protocol.SnapshotMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * הופך את מצב המשחק (Board+GameEngine) להודעת {@link SnapshotMessage} לשידור -
 * שכבת "תרגום דומיין ל-DTO" גרידא. הוצא מ-{@link GameSession} (ר' PROGRESS.md,
 * "פיצול GameSession") - זו הייתה אחת מחמש אחריויות שונות שישבו שם ביחד
 * (חיבורים/ניתוקים/restart/snapshot/ELO), וזו האחת שהכי פשוט להוציא כי היא
 * טהורה: קלט (board+engine+controller+כמה ערכים) בלבד, פלט DTO, בלי state
 * משלה בין קריאות.
 * <p>
 * הפרטים שבאמת שייכים ל"מושב" (session) עצמו - האם ביקשתי restart, כמה
 * שניות נשארו לניתוק, האם ממתינים ליריב - <b>לא</b> מחושבים כאן; הם מגיעים
 * כפרמטרים מוכנים מ-{@code GameSession}, כי הם תלויים ב-state של חיבורי
 * הרשת (connections/pendingDisconnects) שלא קשור בכלל ל"איך הופכים לוח
 * להודעת JSON".
 */
public class SnapshotBuilder {

    /** תוצאת סריקת הלוח: הכלים לשידור + מיקום כל כלי (זהות, לא ערך) - דרוש כדי לאתר קפיצות (ר' collectJumps). */
    private record BoardScan(List<PieceDto> pieces, Map<Piece, Position> positionByPiece) {
    }

    public SnapshotMessage build(Board board, GameEngine engine, GameCommandController commandController,
                                  ClientRole viewerRole, boolean restartRequestedByViewer,
                                  Integer disconnectSecondsRemaining, boolean waitingForOpponent) {
        BoardScan scan = scanBoard(board);
        Optional<Position> selected = viewerRole.toPieceColor()
                .flatMap(commandController::selectedPositionFor);
        List<Position> legalMoves = selected.map(engine::legalMovesFrom).orElse(List.of());
        String winner = engine.winner().map(PieceColor::name).orElse(null);

        return new SnapshotMessage(board.width(), board.height(), scan.pieces(), selected.orElse(null), legalMoves,
                scoresByName(engine), moveLogByName(engine), engine.isGameOver(), winner, engine.now(),
                engine.activeMotions(), collectJumps(engine, scan.positionByPiece()), engine.recentCaptureEffects(),
                restartRequestedByViewer, disconnectSecondsRemaining, waitingForOpponent);
    }

    // סורק את כל הלוח (row/col) פעם אחת - אוסף גם PieceDto לשידור וגם piece->position לצורך collectJumps.
    private BoardScan scanBoard(Board board) {
        List<PieceDto> pieces = new ArrayList<>();
        Map<Piece, Position> positionByPiece = new HashMap<>();
        for (int row = 0; row < board.height(); row++) {
            for (int col = 0; col < board.width(); col++) {
                Position position = new Position(row, col);
                board.pieceAt(position).ifPresent(piece -> {
                    pieces.add(PieceDto.from(piece, position));
                    positionByPiece.put(piece, position);
                });
            }
        }
        return new BoardScan(pieces, positionByPiece);
    }

    // ממיר את כל הקפיצות הפעילות ל-DTO; JumpVisual לא יודע את מיקומו בעצמו, לכן משתמשים במפה מ-scanBoard.
    private List<JumpDto> collectJumps(GameEngine engine, Map<Piece, Position> positionByPiece) {
        List<JumpDto> jumps = new ArrayList<>();
        for (JumpVisual jump : engine.activeJumps()) {
            Position position = positionByPiece.get(jump.piece());
            if (position != null) {
                jumps.add(JumpDto.from(jump, position));
            }
        }
        return jumps;
    }

    // ממיר Map<PieceColor,Integer> של הניקוד ל-Map<String,Integer> לפי שם הצבע, לשידור ב-JSON.
    private Map<String, Integer> scoresByName(GameEngine engine) {
        Map<String, Integer> byName = new HashMap<>();
        engine.scores().forEach((color, score) -> byName.put(color.name(), score));
        return byName;
    }

    // ממיר Map<PieceColor,List<String>> של יומן המהלכים ל-Map<String,List<String>>, לשידור ב-JSON.
    private Map<String, List<String>> moveLogByName(GameEngine engine) {
        Map<String, List<String>> byName = new HashMap<>();
        engine.moveLog().forEach((color, log) -> byName.put(color.name(), log));
        return byName;
    }
}
