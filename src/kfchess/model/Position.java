package kfchess.model;

public class Position {
    private int row;
    private int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }


    public int getRow() {
        return row;
    }



    public void setRow(int row) {
        this.row = row;
    }



    public int getCol() {
        return col;
    }



    public void setCol(int col) {
        this.col = col;
    }



    @Override
    public String toString() {
        return "Position [row=" + row + ", col=" + col + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + row;
        result = prime * result + col;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Position other = (Position) obj;
        
        return row == other.row && col == other.col;
    }

    
}
