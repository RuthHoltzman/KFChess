package kfchess.input;

import kfchess.model.Position;


/** Converts pixel coordinates on the board view into a logical board Position. */
public class BoardMapper {

    /** Which board square a pixel coordinate falls into, given the current cell size. */
    public Position pixelToPosition(int x, int y, int cellWidth, int cellHeight) {
        return new Position(y / cellHeight, x / cellWidth);
    }
}
