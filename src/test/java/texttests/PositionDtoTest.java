package texttests;

import kfchess.model.Position;
import kfchess.net.PositionDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionDtoTest {

    @Test
    void from_copiesRowAndColFromDomainPosition() {
        Position position = new Position(3, 5);

        PositionDto dto = PositionDto.from(position);

        assertEquals(3, dto.row());
        assertEquals(5, dto.col());
    }
}
