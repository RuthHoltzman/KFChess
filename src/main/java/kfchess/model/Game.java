package kfchess.model;

/**
 * ישות המודל המרכזית: אוגדת את מצב המשחק (הלוח + האם המשחק הסתיים).
 * זהו Data Holder טהור בכוונה - כל הלוגיקה העסקית (איך המשחק מתקדם,
 * מתי מהלך נחשב חוקי, מה קורה כשמלך נלכד) נמצאת ב-kfchess.engine.GameEngine.
 * ההפרדה הזו (מה המצב מול איך משנים אותו) היא מה שמאפשר לבדוק את הלוגיקה
 * בקלות ב-Unit Tests בלי תלות ב-IO.
 */
public class Game {

    private final Board board;
    private boolean gameOver = false;

    public Game(Board board) {
        this.board = board;
    }

    public Board board() {
        return board;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void markGameOver() {
        this.gameOver = true;
    }
}
