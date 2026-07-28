package kfchess.app;

import kfchess.account.Account;
import kfchess.account.AccountRepository;
import kfchess.account.SqliteAccountRepository;
import kfchess.account.UsernameTakenException;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;


/** Login/register screen: the only main() entry point for running the client. */
public class LoginScreenMain {

    /** Application entry point: opens the login/register screen. */
    public static void main(String[] args) {
        AccountRepository repository = new SqliteAccountRepository(SqliteAccountRepository.DEFAULT_DB_FILE);
        SwingUtilities.invokeLater(() -> buildAndShow(repository));
    }

    /** Checks that both username and password were actually entered. */
    public static Optional<String> validate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.of("Username is required");
        }
        if (password == null || password.isEmpty()) {
            return Optional.of("Password is required");
        }
        return Optional.empty();
    }

    /** Builds and displays the login/register UI. */
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


    /** Handles the Login button: validates input, then authenticates against the repository. */
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
            HomeScreen.launch(account.get());
        } else {
            showFailure(statusLabel, "Invalid username or password");
        }
    }


    /** Handles the Register button: validates input, then creates a new account. */
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
            HomeScreen.launch(account);
        } catch (UsernameTakenException usernameTaken) {
            showFailure(statusLabel, usernameTaken.getMessage());
        }
    }


    /** Shows a validation or authentication error message. */
    private static void showFailure(JLabel statusLabel, String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
    }
}
