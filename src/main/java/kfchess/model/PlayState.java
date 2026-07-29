package kfchess.model;

import java.util.Optional;

/** Pure data holder for game state (board + game-over + winner); all rules/logic live in PlayEngine instead. */
public class PlayState {

    private final Board board;
    private boolean gameOver = false;
    private PieceColor winner;

    public PlayState(Board board) {
        this.board = board;
    }

    public Board board() {
        return board;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** Marks the game over and records the winner, so the UI can show "White/Black Wins!" not just "Game Over". */
    public void markGameOver(PieceColor winner) {
        this.gameOver = true;
        this.winner = winner;
    }

    public Optional<PieceColor> winner() {
        return Optional.ofNullable(winner);
    }
}
