package kfchess.app;

import kfchess.account.Account;
import kfchess.client.GameClient;
import kfchess.protocol.ConnectionPaths;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/** Home screen: lets the player Play (matchmaking) or open the Room dialog (Create/Join). */
public class HomeScreen {

    private static final String SERVER_HOST_AND_PORT = "ws://localhost:8887";
    private static final long GAME_ID_WAIT_TIMEOUT_MILLIS = 2000;
    private static final long GAME_ID_POLL_INTERVAL_MILLIS = 20;

    /** Opens the home screen window for the given account (or anonymously, if null). */
    public static void launch(Account account) {
        SwingUtilities.invokeLater(() -> buildAndShow(account));
    }

    /** Builds the server WebSocket URI for a specific room, with the username as an optional query parameter. */
    public static String buildUri(String room, String username) {
        String trimmed = room == null ? "" : room.trim();
        String resolvedRoom = trimmed.isEmpty() ? ConnectionPaths.DEFAULT_ROOM : trimmed;
        String base = SERVER_HOST_AND_PORT + "/" + resolvedRoom;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }


    /** Builds the server WebSocket URI for the Play/matchmaking endpoint. */
    public static String buildMatchmakingUri(String username) {
        String base = SERVER_HOST_AND_PORT + "/" + ConnectionPaths.MATCHMAKING;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }


    /** Builds the server WebSocket URI for creating a brand-new room. */
    public static String buildCreateRoomUri(String username) {
        String base = SERVER_HOST_AND_PORT + "/" + ConnectionPaths.CREATE_ROOM;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }


    /** Builds and displays the home screen UI (Play button + Room... button). */
    private static void buildAndShow(Account account) {
        JFrame frame = new JFrame("KFChess - Home");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton playButton = new JButton("Play");
        JButton roomButton = new JButton("Room...");
        JLabel statusLabel = new JLabel(" ");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        if (account != null) {
            panel.add(new JLabel("Logged in as: " + account.username() + " (ELO " + account.elo() + ")"));
            panel.add(Box.createVerticalStrut(8));
        }
        panel.add(playButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(roomButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(statusLabel);

        String username = account == null ? null : account.username();
        playButton.addActionListener(e ->
                connect(frame, buildMatchmakingUri(username), username, statusLabel, playButton, roomButton));
        roomButton.addActionListener(e -> showRoomDialog(frame, username, statusLabel, playButton, roomButton));

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    /** Shows the Room dialog: text field + Create/Join/Cancel buttons. */
    private static void showRoomDialog(JFrame homeFrame, String username, JLabel homeStatusLabel,
                                        JButton playButton, JButton roomButton) {
        JDialog dialog = new JDialog(homeFrame, "Room", true);
        JTextField roomIdField = new JTextField(15);
        JButton createButton = new JButton("Create");
        JButton joinButton = new JButton("Join");
        JButton cancelButton = new JButton("Cancel");
        JLabel dialogStatusLabel = new JLabel(" ");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("Room ID (for Join):"));
        panel.add(roomIdField);
        panel.add(Box.createVerticalStrut(8));

        JPanel buttonsRow = new JPanel();
        buttonsRow.add(createButton);
        buttonsRow.add(joinButton);
        buttonsRow.add(cancelButton);
        panel.add(buttonsRow);
        panel.add(Box.createVerticalStrut(8));
        panel.add(dialogStatusLabel);

        createButton.addActionListener(e -> {
            dialog.dispose();
            connect(homeFrame, buildCreateRoomUri(username), username, homeStatusLabel, playButton, roomButton);
        });
        joinButton.addActionListener(e -> {

            if (roomIdField.getText().isBlank()) {
                dialogStatusLabel.setForeground(Color.RED);
                dialogStatusLabel.setText("Enter a room ID to join");
                return;
            }
            dialog.dispose();
            connect(homeFrame, buildUri(roomIdField.getText(), username), username, homeStatusLabel, playButton, roomButton);
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(homeFrame);
        dialog.setVisible(true);
    }


    /** Connects to the server on a background thread, then opens the game window on success. */
    private static void connect(JFrame homeFrame, String uriText, String username, JLabel statusLabel,
                                 JButton... buttonsToToggle) {
        setButtonsEnabled(buttonsToToggle, false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Connecting...");

        new Thread(() -> {
            GameClient client;
            boolean connected;
            try {
                client = new GameClient(new URI(uriText));
                connected = client.connectBlocking();
            } catch (URISyntaxException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> showFailure(statusLabel, ex.getMessage(), buttonsToToggle));
                return;
            }

            GameClient finalClient = client;
            if (connected) {
                String gameId = waitForAssignedGameId(finalClient);
                SwingUtilities.invokeLater(() -> {
                    homeFrame.dispose();
                    NetworkGameWindow.launch(finalClient, gameId, username);
                });
            } else {
                SwingUtilities.invokeLater(() -> showFailure(statusLabel, "failed to connect to " + uriText, buttonsToToggle));
            }
        }, "home-screen-connect").start();
    }


    /** Polls the client until the server assigns a game id, or the timeout elapses. */
    private static String waitForAssignedGameId(GameClient client) {
        long deadline = System.currentTimeMillis() + GAME_ID_WAIT_TIMEOUT_MILLIS;
        while (client.assignedGameId() == null && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(GAME_ID_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return client.assignedGameId();
    }


    /** Shows a connection failure message and re-enables the buttons. */
    private static void showFailure(JLabel statusLabel, String message, JButton... buttonsToToggle) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
        setButtonsEnabled(buttonsToToggle, true);
    }

    /** Enables or disables a set of buttons at once. */
    private static void setButtonsEnabled(JButton[] buttons, boolean enabled) {
        for (JButton button : buttons) {
            button.setEnabled(enabled);
        }
    }
}
