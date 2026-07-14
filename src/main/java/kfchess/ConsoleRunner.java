package kfchess;

import kfchess.input.Controller;

import java.util.Scanner;

/**
 * לולאת ה-I/O הראשית: קוראת שורות מ-stdin עד סוף הקלט, מפענחת כל שורה
 * ל-Command (דרך Controller/CommandParser), ומאצילה את הביצוע ל-CommandRunner.
 * <p>
 * בכוונה לא בודקת כאן game.isGameOver() - הלולאה ממשיכה לקרוא עד סוף
 * הקלט תמיד (כדי לא "לבלוע" פקודות "print board" שמגיעות אחרי סיום
 * המשחק). האחריות שלא לבצע עוד מהלכים אחרי Game Over היא של GameEngine
 * עצמו (הוא כבר בודק isGameOver בתוך handleClick/handleWait).
 */
public class ConsoleRunner {

    private final Scanner scanner;
    private final Controller parser;
    private final CommandRunner runner;

    public ConsoleRunner(Scanner scanner, Controller parser, CommandRunner runner) {
        this.scanner = scanner;
        this.parser = parser;
        this.runner = runner;
    }

    public void run() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) {
                continue;
            }
            runner.run(parser.parse(line));
        }
    }
}