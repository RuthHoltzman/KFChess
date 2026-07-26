package kfchess.net;

import kfchess.engine.snapshot.JumpVisual;
import kfchess.model.Position;

/**
 * מצמיד קפיצה פעילה למיקומה על הלוח - JumpVisual (כמו Piece) לא יודע
 * את מיקומו בעצמו, ולכן GameSession מצרף אותו מבחוץ (ר' scanBoard).
 * Gson מסריאלז את jump/at ישירות, בלי העתקת שדות ידנית.
 */
public class JumpDto {

    private final Position at;
    private final JumpVisual jump;

    public JumpDto(Position at, JumpVisual jump) {
        this.at = at;
        this.jump = jump;
    }

    public static JumpDto from(JumpVisual jump, Position position) {
        return new JumpDto(position, jump);
    }

    // צריך רק בצד הלקוח - ר' PieceDto.piece()/position() לאותה סיבה.
    public Position at() {
        return at;
    }

    public JumpVisual jump() {
        return jump;
    }
}
