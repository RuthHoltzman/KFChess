package kfchess.rules;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceKind;
import kfchess.model.Position;

/**
 * שכבת האכיפה שמעל PieceRules: בודקת תנאים כלליים שנכונים לכל כלי
 * (גבולות הלוח, לא לתפוס כלי מאותו צבע) ורק אז מאצילה את הבדיקה
 * הספציפית-לכלי ל-MoveRule המתאים.
 */
public class RuleEngine {

    private final PieceRules pieceRules;

    public RuleEngine(PieceRules pieceRules) {
        this.pieceRules = pieceRules;
    }

    public RuleEngine() {
        this(new PieceRules());
    }

    public boolean isLegalMove(Board board, Piece piece, Position from, Position to) {
        if (!board.isWithinBounds(to)) {
            return false;
        }
        if (board.isOccupiedBySameColor(to, piece.color())) {
            return false;
        }
        return pieceRules.ruleFor(piece.kind()).isLegal(board, from, to);
    }

    /**
     * נקודת ההרחבה לחוקים דינמיים (דרישה 6ב'): מאפשרת להזריק/להחליף
     * MoveRule עבור סוג כלי מסוים בזמן ריצה, מבלי לגעת בקוד הקיים.
     */
    public void registerCustomRule(PieceKind kind, PieceRules.MoveRule rule) {
        pieceRules.register(kind, rule);
    }

    /**
     * בודקת שהמסלול הישר בין from ל-to פנוי מכלים חוסמים (לא כולל היעד עצמו).
     * חבילתית (package-private) - נחשפת רק לכללי התנועה בתוך kfchess.rules,
     * כדי לא לדלוף לוגיקת "מסלול" החוצה לחבילות אחרות.
     */
    static boolean isPathClear(Board board, Position from, Position to) {
        int stepRow = Integer.compare(to.row(), from.row());
        int stepCol = Integer.compare(to.col(), from.col());

        Position current = from.offset(stepRow, stepCol);
        while (!current.equals(to)) {
            if (!board.isEmpty(current)) {
                return false;
            }
            current = current.offset(stepRow, stepCol);
        }
        return true;
    }
}
