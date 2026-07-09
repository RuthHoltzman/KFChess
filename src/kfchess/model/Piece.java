package kfchess.model;

public class Piece {

    private static int idCounter = 0;

    // Fields
    private final String id;
    private final PieceColor color;
    private final PieceKind kind;
    private Position cell;       // Uses your Position class
    private PieceState state;

    // Constructor
    public Piece( PieceColor color, PieceKind kind, Position cell) {
        this.id = "PIECE_" + ++idCounter;
        this.color = color;
        this.kind = kind;
        this.cell = cell;
        this.state = PieceState.IDLE; // Default lifecycle state as implied
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public PieceColor getColor() {
        return color;
    }

    public PieceKind getKind() {
        return kind;
    }

    public Position getCell() {
        return cell;
    }

    public void setCell(Position cell) {
        this.cell = cell;
    }

    public PieceState getState() {
        return state;
    }

    public void setState(PieceState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Piece [id=" + id + ", color=" + color + ", kind=" + kind + ", cell=" + cell + ", state=" + state + "]";
    }
    
}