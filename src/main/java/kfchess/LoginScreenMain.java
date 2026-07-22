package kfchess;

import kfchess.account.Account;
import kfchess.account.AccountRepository;
import kfchess.account.SqliteAccountRepository;
import kfchess.account.UsernameTakenException;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * מסך login/register (שלב 4, "v1") - חלון Swing נפרד לפני מסך הבית
 * (HomeScreenMain), באותו סגנון פשוט בדיוק (BoxLayout, בלי עיצוב מיוחד).
 * Login ו-Register הם שני כפתורים נפרדים (לא auto-register) - מישהי
 * שמקלידה username שלא קיים ולוחצת Login מקבלת שגיאה, לא חשבון חדש
 * בלי כוונה. גישה לקובץ ה-DB היא סינכרונית (בניגוד ל-connect() ב-
 * HomeScreenMain) כי קריאה/כתיבה ל-SQLite מקומי מהירה מספיק שלא צריך
 * thread נפרד כדי לא להקפיא את ה-EDT.
 */
public class LoginScreenMain {

    private static final String DB_FILE = "kfchess.db";

    public static void main(String[] args) {
        AccountRepository repository = new SqliteAccountRepository(DB_FILE);
        SwingUtilities.invokeLater(() -> buildAndShow(repository));
    }

    // בודקת שדות ריקים/רק-רווחים לפני שפונים בכלל ל-repository - פונקציה
    // טהורה ונפרדת מה-UI כדי שתהיה ניתנת לבדיקה בלי להרים חלון Swing
    // (כמו buildUri ב-HomeScreenMain).
    public static Optional<String> validate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.of("Username is required");
        }
        if (password == null || password.isEmpty()) {
            return Optional.of("Password is required");
        }
        return Optional.empty();
    }

    private static void buildAndShow(AccountRepository repository) {
        JFrame frame = new JFrame("KFChess - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JLabel statusLabel = new JLabel(" ");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(loginButton);
        buttonsPanel.add(registerButton);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(buttonsPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(statusLabel);

        loginButton.addActionListener(e ->
                handleLogin(frame, repository, usernameField, passwordField, statusLabel));
        registerButton.addActionListener(e ->
                handleRegister(frame, repository, usernameField, passwordField, statusLabel));

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // מטפל בלחיצה על Login: מאמת שדות, ואז שואל את ה-repository אם
    // username+password תואמים לחשבון קיים - הצלחה סוגרת את מסך ה-login
    // ופותחת את מסך הבית עם ה-Account שהתקבל (HomeScreenMain.launch).
    private static void handleLogin(JFrame frame, AccountRepository repository, JTextField usernameField,
                                     JPasswordField passwordField, JLabel statusLabel) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        Optional<String> validationError = validate(username, password);
        if (validationError.isPresent()) {
            showFailure(statusLabel, validationError.get());
            return;
        }

        Optional<Account> account = repository.login(username.trim(), password);
        if (account.isPresent()) {
            frame.dispose();
            HomeScreenMain.launch(account.get());
        } else {
            showFailure(statusLabel, "Invalid username or password");
        }
    }

    // מטפל בלחיצה על Register: מאמת שדות, ואז מנסה ליצור חשבון חדש -
    // UsernameTakenException (מ-SqliteAccountRepository) מוצגת כשגיאה
    // רגילה ב-label, בדיוק כמו כל שגיאה אחרת כאן.
    private static void handleRegister(JFrame frame, AccountRepository repository, JTextField usernameField,
                                        JPasswordField passwordField, JLabel statusLabel) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        Optional<String> validationError = validate(username, password);
        if (validationError.isPresent()) {
            showFailure(statusLabel, validationError.get());
            return;
        }

        try {
            Account account = repository.register(username.trim(), password);
            frame.dispose();
            HomeScreenMain.launch(account);
        } catch (UsernameTakenException usernameTaken) {
            showFailure(statusLabel, usernameTaken.getMessage());
        }
    }

    // מציגה הודעת כישלון בחלון ה-login עצמו (label קיים, בלי popup) - אותה
    // גישה בדיוק כמו HomeScreenMain.showFailure.
    private static void showFailure(JLabel statusLabel, String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }
}
