package kfchess.model;

import java.util.Map;

public class Board {

    private int width;

    private int height;
    private final Map<Position, Piece> pieces;

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.pieces = new java.util.HashMap<>();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    
    
}

// הוספת כלי
// הסרת כלי
//שליפת כלי לפי מיקום
//בדיקת גבולות הלוח
//הזזת כלי

