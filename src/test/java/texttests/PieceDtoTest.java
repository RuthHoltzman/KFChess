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

class PieceDtoTest {

    private final Gson gson = new Gson();

    @Test
    void from_mapsColorKindStateAndPosition_asEnumNames() {
        Piece piece = new Piece(PieceColor.BLACK, PieceKind.KNIGHT);

        PieceDto dto = PieceDto.from(piece, new Position(1, 2));
        JsonObject json = gson.toJsonTree(dto).getAsJsonObject();

        assertEquals("BLACK", json.get("color").getAsString());
        assertEquals("KNIGHT", json.get("kind").getAsString());
        assertEquals("IDLE", json.get("state").getAsString());
        assertEquals(1, json.get("row").getAsInt());
        assertEquals(2, json.get("col").getAsInt());
    }
}
