package kfchess.view;

import kfchess.model.PieceColor;

import java.awt.Color;
import java.util.List;

/** Draws one player's side panel (score + move list). Deliberately "dumb" - it takes ready values, not game objects. */
public class SidePanelView {

    /** Per-side color scheme, so the eye distinguishes White from Black without reading the label. */
    private record Theme(Color background, Color border, Color headerText, Color moveText,
                          Color badgeBackground, Color badgeText) {}

    private static final Theme WHITE_THEME = new Theme(
            new Color(246, 243, 236), new Color(200, 190, 170),
            new Color(44, 44, 42), new Color(95, 94, 90),
            new Color(232, 223, 200), new Color(95, 77, 31));

    private static final Theme BLACK_THEME = new Theme(
            new Color(35, 35, 35), new Color(70, 70, 68),
            new Color(232, 230, 223), new Color(168, 166, 158),
            new Color(60, 74, 92), new Color(205, 222, 240));

    private static final int PADDING = 14;
    private static final int HEADER_FONT_SIZE = 22;
    private static final int BADGE_FONT_SIZE = 12;
    private static final int SCORE_FONT_SIZE = 18;
    private static final int MOVE_FONT_SIZE = 15;
    private static final int MOVE_LINE_HEIGHT = 22;
    private static final int HEADER_Y = 32;
    // The "You" badge sits below the header. Its space stays reserved even when not drawn,
    // so both panels stay aligned regardless of which one is the local player's.
    private static final int BADGE_TOP_Y = 40;
    private static final int BADGE_HEIGHT = 18;
    private static final int SCORE_Y = 72;
    private static final int MOVES_TITLE_Y = 106;
    private static final int MOVES_START_Y = 130;

    private final int panelWidth;

    public SidePanelView(int panelWidth) {
        this.panelWidth = panelWidth;
    }

    /** The panel's fixed width in pixels. */
    public int panelWidth() {
        return panelWidth;
    }

    /** Draws the whole panel at the given offset; every Y constant is relative to startY, so it moves as one block. */
    public void draw(Img canvas, int offsetX, int startY, int panelHeight,
                      PieceColor color, int score, List<String> moves,
                      boolean isLocalPlayer, String username) {
        Theme theme = color == PieceColor.WHITE ? WHITE_THEME : BLACK_THEME;

        canvas.fillRect(offsetX, startY, panelWidth, panelHeight, theme.background());
        canvas.drawRect(offsetX, startY, panelWidth, panelHeight, theme.border(), 2);

        String title = displayName(color);
        canvas.drawText(title, offsetX + PADDING, startY + HEADER_Y, HEADER_FONT_SIZE, theme.headerText(), true);

        if (isLocalPlayer) {
            drawYouBadge(canvas, offsetX, startY, theme, username);
        }

        canvas.drawText("Score: " + score, offsetX + PADDING, startY + SCORE_Y, SCORE_FONT_SIZE, theme.headerText(), false);
        canvas.drawText("Moves:", offsetX + PADDING, startY + MOVES_TITLE_Y, SCORE_FONT_SIZE, theme.headerText(), true);

        int maxVisibleRows = Math.max(0, (panelHeight - MOVES_START_Y - PADDING) / MOVE_LINE_HEIGHT);
        List<String> recentMoves = lastN(moves, maxVisibleRows);

        // Newest move first (at the top) - no scrolling needed, the panel has a fixed height anyway.
        int y = startY + MOVES_START_Y;
        for (int i = recentMoves.size() - 1; i >= 0; i--) {
            canvas.drawText(recentMoves.get(i), offsetX + PADDING, y, MOVE_FONT_SIZE, theme.moveText(), false);
            y += MOVE_LINE_HEIGHT;
        }
    }

    /** Draws the rounded "You: username" badge, sized from the measured text so any username fits. */
    private void drawYouBadge(Img canvas, int offsetX, int startY, Theme theme, String username) {
        String text = username != null ? "You: " + username : "You";
        int textWidth = canvas.textWidth(text, BADGE_FONT_SIZE, true);
        int badgeWidth = textWidth + PADDING;
        int badgeY = startY + BADGE_TOP_Y;
        canvas.fillRoundRect(offsetX + PADDING, badgeY, badgeWidth, BADGE_HEIGHT,
                BADGE_HEIGHT, BADGE_HEIGHT, theme.badgeBackground());
        int textY = badgeY + BADGE_HEIGHT - 5;
        canvas.drawText(text, offsetX + PADDING + PADDING / 2, textY, BADGE_FONT_SIZE, theme.badgeText(), true);
    }

    /** The panel header text for a color. */
    private static String displayName(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }

    /** The last n entries of the list, or fewer if it's shorter. */
    private static List<String> lastN(List<String> list, int n) {
        if (n <= 0 || list.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, list.size() - n);
        return list.subList(from, list.size());
    }
}
