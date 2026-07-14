package kfchess.view;

import kfchess.model.Piece;
import kfchess.model.Position;
import java.awt.*;
import java.util.Map;

public class BoardView {

    private final Img boardImg;
    private final BoardGeometry geometry;

    public BoardView(Img boardImg, BoardGeometry geometry) {
        this.boardImg = boardImg;
        this.geometry = geometry;
    }

   public void render(Map<Position, Piece> pieces, long elapsedMillis) {
    for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
        Position pos = entry.getKey();
        Piece piece = entry.getValue();

        String framePath = currentFramePathFor(piece, elapsedMillis);

        Img pieceImg = new Img().read(
            framePath,
            new Dimension(geometry.getCellWidth(), geometry.getCellHeight()),
            true, null
        );
        Point p = geometry.cellToPixel(pos);
        pieceImg.drawOn(boardImg, p.x, p.y);
    }
    boardImg.show();
}
    // private static String imagePathFor(Piece piece) {
    // String folder = "" + piece.kind().code() + Character.toUpperCase(piece.color().code());
    // return "src/main/resources/pieces/" + folder + "/states/idle/sprites/1.png";
    // }  
    
private String currentFramePathFor(Piece piece, long elapsedMillis) {
    String folder = "" + piece.kind().code() + Character.toUpperCase(piece.color().code());
    String spritesFolder = "src/main/resources/pieces/" + folder + "/states/idle/sprites";

    AnimationClip clip = AnimationClipCache.get(spritesFolder, 6, true);
    int frameIndex = clip.getFrameIndex(elapsedMillis);
    return clip.getFramePath(frameIndex);
}

}