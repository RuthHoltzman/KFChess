package texttests;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import kfchess.protocol.JumpDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JumpDto רק מצמיד jump+position (JumpVisual, כמו Piece, לא יודע את
 * מיקומו בעצמו) - Motion ו-CaptureEffect לעומת זאת נשלחים היום ישירות
 * בלי DTO עוטף, כי כבר יש להם את כל השדות הדרושים (ר' ההחלטה לצמצם
 * DTOs מיותרים ב-PROGRESS.md).
 */
class JumpDtoTest {

    private final Gson gson = new Gson();

    @Test
    void from_bundlesJumpAndPosition_forSerialization() {
        Piece piece = new Piece(PieceColor.BLACK, PieceKind.KNIGHT);
        JumpVisual jump = new JumpVisual(piece, 200L, 700L);

        JumpDto dto = JumpDto.from(jump, new Position(2, 3));
        JsonObject json = gson.toJsonTree(dto).getAsJsonObject();

        JsonObject atJson = json.getAsJsonObject("at");
        assertEquals(2, atJson.get("row").getAsInt());
        assertEquals(3, atJson.get("col").getAsInt());

        JsonObject jumpJson = json.getAsJsonObject("jump");
        assertEquals(200, jumpJson.get("startTime").getAsLong());
        assertEquals(700, jumpJson.get("endTime").getAsLong());
    }
}
