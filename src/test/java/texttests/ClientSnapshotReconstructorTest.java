package texttests;

import com.google.gson.Gson;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import kfchess.net.ClientSnapshotReconstructor;
import kfchess.net.IncomingSnapshot;
import kfchess.net.JumpDto;
import kfchess.net.PieceDto;
import kfchess.net.SnapshotMessage;
import kfchess.realtime.Motion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * בודק את ClientSnapshotReconstructor דרך round-trip אמיתי (SnapshotMessage
 * -> JSON עם Gson -> IncomingSnapshot), בדיוק כמו שקורה בפועל בין שרת
 * ללקוח - לא בונה IncomingSnapshot ידנית. שני הדברים החשובים ביותר
 * שנבדקים כאן: (1) שכלי עם אותו piece.id() נשאר אותו אובייקט Java בין
 * שתי הודעות נפרדות (שימור זהות - זה מה שמאפשר את אנימציית שעון-החול
 * ברשת), ו-(2) שהכלי על הלוח וה-Piece בתוך Motion/JumpVisual המשוחזרים
 * הם אותו אובייקט בדיוק בתוך אותה הודעה (בלעדי זה SnapshotFactory לא
 * יכול לקשר תנועה לכלי בכלל, גם בלי קשר לזהות בין הודעות).
 */
class ClientSnapshotReconstructorTest {

    private final Gson gson = new Gson();

    private IncomingSnapshot roundTrip(SnapshotMessage message) {
        return gson.fromJson(gson.toJson(message), IncomingSnapshot.class);
    }

    @Test
    void reconstruct_newPiece_placedOnBoardAtCorrectPosition() {
        Piece king = new Piece(PieceColor.WHITE, PieceKind.KING);
        SnapshotMessage message = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(king, new Position(0, 4))), null, List.of(), Map.of(), Map.of(),
                false, null, 1000L, List.of(), List.of(), List.of());

        ClientSnapshotReconstructor.Reconstructed result =
                new ClientSnapshotReconstructor().reconstruct(roundTrip(message));

        assertEquals(8, result.board().width());
        assertEquals(8, result.board().height());
        Piece placed = result.board().pieceAt(new Position(0, 4)).orElseThrow();
        assertEquals(PieceKind.KING, placed.kind());
        assertEquals(PieceColor.WHITE, placed.color());
    }

    @Test
    void reconstruct_nonStandardBoardSize_notHardCoded() {
        SnapshotMessage message = new SnapshotMessage(5, 6,
                List.of(), null, List.of(), Map.of(), Map.of(), false, null, 0L, List.of(), List.of(), List.of());

        ClientSnapshotReconstructor.Reconstructed result =
                new ClientSnapshotReconstructor().reconstruct(roundTrip(message));

        assertEquals(5, result.board().width());
        assertEquals(6, result.board().height());
    }

    @Test
    void reconstruct_samePieceIdAcrossTwoMessages_reusesSameObjectIdentity() {
        Piece pawn = new Piece(PieceColor.WHITE, PieceKind.PAWN);
        SnapshotMessage message1 = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(pawn, new Position(6, 4))), null, List.of(), Map.of(), Map.of(),
                false, null, 0L, List.of(), List.of(), List.of());
        SnapshotMessage message2 = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(pawn, new Position(6, 4))), null, List.of(), Map.of(), Map.of(),
                false, null, 16L, List.of(), List.of(), List.of());

        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        Piece first = reconstructor.reconstruct(roundTrip(message1)).board()
                .pieceAt(new Position(6, 4)).orElseThrow();
        Piece second = reconstructor.reconstruct(roundTrip(message2)).board()
                .pieceAt(new Position(6, 4)).orElseThrow();

        assertSame(first, second);
    }

    @Test
    void reconstruct_pieceArrival_preservesIdentityAndUpdatesStateViaMarkArrived() {
        Piece pawn = new Piece(PieceColor.WHITE, PieceKind.PAWN);
        pawn.markInTransit();
        Motion motion = new Motion(pawn, new Position(6, 4), new Position(5, 4), 0L, 1000L);
        SnapshotMessage message1 = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(pawn, new Position(6, 4))), null, List.of(), Map.of(), Map.of(),
                false, null, 500L, List.of(motion), List.of(), List.of());

        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        Piece moverDuringTransit = reconstructor.reconstruct(roundTrip(message1)).board()
                .pieceAt(new Position(6, 4)).orElseThrow();
        assertTrue(moverDuringTransit.isInTransit());

        // בדיוק כמו ש-GameEngine.completeMotion עושה בפועל: מקדם את מצב
        // אותו אובייקט Piece עצמו לפני שההודעה הבאה נבנית.
        pawn.markArrived();
        SnapshotMessage message2 = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(pawn, new Position(5, 4))), null, List.of(), Map.of(), Map.of(),
                false, null, 1000L, List.of(), List.of(), List.of());
        Piece moverAfterArrival = reconstructor.reconstruct(roundTrip(message2)).board()
                .pieceAt(new Position(5, 4)).orElseThrow();

        assertSame(moverDuringTransit, moverAfterArrival);
        assertTrue(moverAfterArrival.isIdle());
    }

    @Test
    void reconstruct_jumpArrival_preservesIdentityAndUpdatesStateViaMarkJumpEnded() {
        Piece knight = new Piece(PieceColor.BLACK, PieceKind.KNIGHT);
        knight.markJumping();
        JumpVisual jump = new JumpVisual(knight, 0L, 500L);
        SnapshotMessage message1 = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(knight, new Position(2, 3))), null, List.of(), Map.of(), Map.of(),
                false, null, 200L, List.of(), List.of(JumpDto.from(jump, new Position(2, 3))), List.of());

        ClientSnapshotReconstructor reconstructor = new ClientSnapshotReconstructor();
        Piece jumperMidAir = reconstructor.reconstruct(roundTrip(message1)).board()
                .pieceAt(new Position(2, 3)).orElseThrow();
        assertTrue(jumperMidAir.isJumping());

        knight.markJumpEnded();
        SnapshotMessage message2 = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(knight, new Position(2, 3))), null, List.of(), Map.of(), Map.of(),
                false, null, 500L, List.of(), List.of(), List.of());
        Piece jumperLanded = reconstructor.reconstruct(roundTrip(message2)).board()
                .pieceAt(new Position(2, 3)).orElseThrow();

        assertSame(jumperMidAir, jumperLanded);
        assertTrue(jumperLanded.isIdle());
    }

    @Test
    void reconstruct_motionPieceReference_matchesBoardPieceExactly() {
        Piece rook = new Piece(PieceColor.WHITE, PieceKind.ROOK);
        rook.markInTransit();
        Motion motion = new Motion(rook, new Position(7, 0), new Position(6, 0), 0L, 1000L);
        SnapshotMessage message = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(rook, new Position(7, 0))), null, List.of(), Map.of(), Map.of(),
                false, null, 500L, List.of(motion), List.of(), List.of());

        ClientSnapshotReconstructor.Reconstructed result =
                new ClientSnapshotReconstructor().reconstruct(roundTrip(message));

        Piece onBoard = result.board().pieceAt(new Position(7, 0)).orElseThrow();
        assertSame(onBoard, result.motions().get(0).piece());
    }

    @Test
    void reconstruct_jumpPieceReference_matchesBoardPieceExactly() {
        Piece bishop = new Piece(PieceColor.BLACK, PieceKind.BISHOP);
        bishop.markJumping();
        JumpVisual jump = new JumpVisual(bishop, 0L, 500L);
        SnapshotMessage message = new SnapshotMessage(8, 8,
                List.of(PieceDto.from(bishop, new Position(2, 5))), null, List.of(), Map.of(), Map.of(),
                false, null, 200L, List.of(), List.of(JumpDto.from(jump, new Position(2, 5))), List.of());

        ClientSnapshotReconstructor.Reconstructed result =
                new ClientSnapshotReconstructor().reconstruct(roundTrip(message));

        Piece onBoard = result.board().pieceAt(new Position(2, 5)).orElseThrow();
        assertSame(onBoard, result.jumps().get(0).piece());
    }

    @Test
    void reconstruct_scoresAndMoveLog_convertedFromStringKeysToPieceColor() {
        SnapshotMessage message = new SnapshotMessage(8, 8, List.of(), null, List.of(),
                Map.of("WHITE", 9, "BLACK", 3),
                Map.of("WHITE", List.of("e2e4")),
                false, null, 0L, List.of(), List.of(), List.of());

        ClientSnapshotReconstructor.Reconstructed result =
                new ClientSnapshotReconstructor().reconstruct(roundTrip(message));

        assertEquals(9, result.scores().get(PieceColor.WHITE));
        assertEquals(3, result.scores().get(PieceColor.BLACK));
        assertEquals(List.of("e2e4"), result.moveLog().get(PieceColor.WHITE));
    }
}
