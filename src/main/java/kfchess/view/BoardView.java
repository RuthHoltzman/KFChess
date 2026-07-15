package kfchess.view;

import kfchess.engine.GameSnapshot;
import kfchess.engine.PieceSnapshot;
import kfchess.engine.PieceVisualState;
import java.awt.*;

public class BoardView {

    private final String boardImagePath;
    private final BoardGeometry geometry;

    public BoardView(String boardImagePath, BoardGeometry geometry) {
        this.boardImagePath = boardImagePath;
        this.geometry = geometry;
    }

    public void render(GameSnapshot snapshot) {
        Img canvas = new Img().read(boardImagePath); // <-- קנבס טרי בכל render

        for (PieceSnapshot piece : snapshot.pieces()) {
            String framePath = currentFramePathFor(piece);
            Img pieceImg = new Img().read(
                    framePath,
                    new Dimension(geometry.getCellWidth(), geometry.getCellHeight()),
                    true, null
            );
            int x = (int) Math.round(piece.pixelX());
            int y = (int) Math.round(piece.pixelY());
            pieceImg.drawOn(canvas, x, y);
        }
        canvas.show();
    }

   

    private String currentFramePathFor(PieceSnapshot piece) {
        String folder = "" + piece.kind().code() + Character.toUpperCase(piece.color().code());
        String stateFolder = stateFolderFor(piece.state());
        String spritesFolder = "src/main/resources/pieces/" + folder + "/states/" + stateFolder + "/sprites";

        AnimationClip clip = AnimationClipCache.get(
                spritesFolder, framesPerSecFor(piece.state()), isLoopFor(piece.state())
        );
        int frameIndex = clip.getFrameIndex(piece.stateElapsedMillis());
        return clip.getFramePath(frameIndex);
    }

    private String stateFolderFor(PieceVisualState state) {
        return switch (state) {
            case IDLE -> "idle";
            case MOVING -> "move";
            case JUMPING -> "jump";
            case SHORT_REST -> "short_rest";
            case LONG_REST -> "long_rest";
        };
    }

    private int framesPerSecFor(PieceVisualState state) {
        return switch (state) {
            case IDLE -> 6;
            case MOVING -> 12;
            case JUMPING, SHORT_REST -> 8;
            case LONG_REST -> 6;
        };
    }

    private boolean isLoopFor(PieceVisualState state) {
        return state != PieceVisualState.JUMPING;
    }
}