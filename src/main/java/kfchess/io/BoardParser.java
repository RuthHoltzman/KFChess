package kfchess.io;

import kfchess.model.Board;
import kfchess.model.Piece;
import kfchess.model.PieceColor;
import kfchess.model.PieceKind;
import kfchess.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * קוראת ומאמתת את מקטע "Board:" מתוך קלט טקסטואלי, ובונה ממנו Board.
 * ה-Scanner מוזרק דרך הבנאי (constructor injection) - לא נוצר כאן
 * ולא נלקח מ-System.in ישירות - כדי שאפשר יהיה לבדוק את המחלקה
 * עם קלט מדומה (ראו kfchess.texttests.BoardParserTest).
 */
public class BoardParser {

    private static final String BOARD_HEADER = "Board:";
    private static final String COMMANDS_HEADER = "Commands:";
    private static final String EMPTY_CELL_TOKEN = ".";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String ERROR_EMPTY_BOARD = "ERROR EMPTY_BOARD";
    private static final String ERROR_ROW_WIDTH_MISMATCH = "ERROR ROW_WIDTH_MISMATCH";
    private static final String ERROR_UNKNOWN_TOKEN = "ERROR UNKNOWN_TOKEN";
    private final Scanner scanner;

    public BoardParser(Scanner scanner) {
        this.scanner = scanner;
    }

    public Board readBoard() {
        List<String[]> rawRows = readRawBoardLines();
        ValidationResult result = isValidBoard(rawRows);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.errorMessage());
        }
        return buildBoard(rawRows);
    }

    private List<String[]> readRawBoardLines() {
        List<String[]> lines = new ArrayList<>();
        boolean readingBoard = false;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase(BOARD_HEADER)) {
                readingBoard = true;
                continue;
            }
            if (line.equalsIgnoreCase(COMMANDS_HEADER) || line.isEmpty()) {
                break;
            }
            if (readingBoard) {
                lines.add(WHITESPACE.split(line));
            }
        }
        return lines;
    }

   private ValidationResult isValidBoard(List<String[]> lines) {
    if (lines == null || lines.isEmpty()) {
        return ValidationResult.failure(ERROR_EMPTY_BOARD);
    }
    if (!hasConsistentRowWidth(lines)) {
        return ValidationResult.failure(ERROR_ROW_WIDTH_MISMATCH);
    }
    if (!hasOnlyValidTokens(lines)) {
        return ValidationResult.failure(ERROR_UNKNOWN_TOKEN);
    }
    return ValidationResult.success();
}
    private boolean hasConsistentRowWidth(List<String[]> lines) {
        int width = lines.get(0).length;
        return lines.stream().allMatch(row -> row.length == width);
    }

    private boolean hasOnlyValidTokens(List<String[]> lines) {
        return lines.stream()
                .flatMap(java.util.Arrays::stream)
                .allMatch(this::isValidToken);
    }

    private boolean isValidToken(String token) {
        if (token.equals(EMPTY_CELL_TOKEN)) {
            return true;
        }
        if (token.length() != 2) {
            return false;
        }
        char colorCode = token.charAt(0);
        char kindCode = token.charAt(1);
        boolean validColor = colorCode == PieceColor.WHITE.code() || colorCode == PieceColor.BLACK.code();
        return validColor && PieceKind.isValidCode(kindCode);
    }

    private Board buildBoard(List<String[]> rows) {
        int height = rows.size();
        int width = rows.get(0).length;
        Board board = Board.createDefault(height, width);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                String token = rows.get(row)[col];
                if (!token.equals(EMPTY_CELL_TOKEN)) {
                    board.placePiece(new Position(row, col), pieceFromToken(token));
                }
            }
        }
        return board;
    }

    private Piece pieceFromToken(String token) {
        PieceColor color = PieceColor.fromCode(token.charAt(0));
        PieceKind kind = PieceKind.fromCode(token.charAt(1));
        return new Piece(color, kind);
    }


    private static final class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    boolean isValid() {
        return valid;
    }

    String errorMessage() {
        return errorMessage;
    }
}
}
