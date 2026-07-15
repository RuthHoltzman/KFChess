package kfchess.model;

import java.util.Optional;

/**
 * ישות המודל המרכזית: אוגדת את מצב המשחק (הלוח + האם המשחק הסתיים + מי ניצח).
 * זהו Data Holder טהור בכוונה - כל הלוגיקה העסקית (איך המשחק מתקדם,
 * מתי מהלך נחשב חוקי, מה קורה כשמלך נלכד) נמצאת ב-kfchess.engine.GameEngine.
 * ההפרדה הזו (מה המצב מול איך משנים אותו) היא מה שמאפשר לבדוק את הלוגיקה
 * בקלות ב-Unit Tests בלי תלות ב-IO.
 */
public class Game {

    private final Board board;
    private boolean gameOver = false;
    private PieceColor winner;

    public Game(Board board) {
        this.board = board;
    }

    public Board board() {
        return board;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * מסמן שהמשחק הסתיים ושומר מי ניצח (הצבע שלכד את המלך היריב) -
     * כדי שה-UI יוכל להציג "White Wins!" / "Black Wins!" ולא רק "המשחק נגמר".
     */
    public void markGameOver(PieceColor winner) {
        this.gameOver = true;
        this.winner = winner;
    }

    public Optional<PieceColor> winner() {
        return Optional.ofNullable(winner);
    }
}
