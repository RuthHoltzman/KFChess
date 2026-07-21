package texttests;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;
import kfchess.net.PieceDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PieceDto רק מצמיד piece+position (Piece לא יודע את מיקומו בעצמו) -
 * Gson מסריאלז את שניהם ישירות, אז הטסט בודק את הצורה המקוננת שיוצאת.
 */
class PieceDtoTest {

    private final Gson gson = new Gson();

    @Test
    void from_bundlesPieceAndPosition_forSerialization() {
        Piece piece = new Piece(PieceColor.BLACK, PieceKind.KNIGHT);

        PieceDto dto = PieceDto.from(piece, new Position(1, 2));
        JsonObject json = gson.toJsonTree(dto).getAsJsonObject();

        JsonObject pieceJson = json.getAsJsonObject("piece");
        assertEquals("BLACK", pieceJson.get("color").getAsString());
        assertEquals("KNIGHT", pieceJson.get("kind").getAsString());
        assertEquals("IDLE", pieceJson.get("state").getAsString());

        JsonObject positionJson = json.getAsJsonObject("position");
        assertEquals(1, positionJson.get("row").getAsInt());
        assertEquals(2, positionJson.get("col").getAsInt());
    }
}
