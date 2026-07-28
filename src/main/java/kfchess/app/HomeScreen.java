package kfchess.app;

import kfchess.account.Account;
import kfchess.client.GameClient;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class HomeScreen {

    private static final String SERVER_HOST_AND_PORT = "ws://localhost:8887";
    private static final String DEFAULT_ROOM = "default";
    private static final String MATCHMAKING_PATH = "_play";
    private static final String CREATE_ROOM_PATH = "_create";
    private static final long GAME_ID_WAIT_TIMEOUT_MILLIS = 2000;
    private static final long GAME_ID_POLL_INTERVAL_MILLIS = 20;
    public static void launch(Account account) {
        SwingUtilities.invokeLater(() -> buildAndShow(account));
    }

    public static String buildUri(String room, String username) {
        String trimmed = room == null ? "" : room.trim();
        String resolvedRoom = trimmed.isEmpty() ? DEFAULT_ROOM : trimmed;
        String base = SERVER_HOST_AND_PORT + "/" + resolvedRoom;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    // בונה URI לבקשת matchmaking (כפתור "Play", שלב 5 חלק 2) - נתיב שמור
    // (MATCHMAKING_PATH); אותה שיטת קידוד username בדיוק כמו buildUri.
    public static String buildMatchmakingUri(String username) {
        String base = SERVER_HOST_AND_PORT + "/" + MATCHMAKING_PATH;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    // בונה URI לבקשת "Create room" (שלב 6, כפתור Create בדיאלוג Room) -
    // נתיב שמור אחר (CREATE_ROOM_PATH) - מתעלם לגמרי משדה טקסט כלשהו,
    // כי ה-gameId נוצר ע"י השרת (ר' GameServer.createNewRoomGameId), לא ע"י הלקוח.
    public static String buildCreateRoomUri(String username) {
        String base = SERVER_HOST_AND_PORT + "/" + CREATE_ROOM_PATH;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    // בונה את חלון הבית עצמו: כפתור "Play" (matchmaking, ר' buildMatchmakingUri)
    // + כפתור "Room..." (פותח דיאלוג Create/Join/Cancel, ר' showRoomDialog)
    // + label לסטטוס/שגיאות, ובנוסף שורת "Logged in as" אם הגיעה לכאן דרך
    // launch(Account) אחרי login (account != null). רץ על ה-EDT (נקראת
    // רק מתוך main()/launch() דרך invokeLater).
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

    // דיאלוג "Room →" (שלב 6): תיבת טקסט + שלושה כפתורים בדיוק לפי המפרט -
    // Create (מתעלם מהתיבה, מבקש מהשרת gameId חדש - ר' buildCreateRoomUri),
    // Join (מתחבר לפי ה-ID שהוקלד - buildUri הרגילה), Cancel (סוגר בלי
    // לעשות כלום). מודלי (modal, ר' הבנאי) כדי שאי אפשר ללחוץ בטעות על
    // "Play" במסך הבית שמתחתיו כל עוד הדיאלוג פתוח.
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
            // Join בלי ID מוקלד לא הגיוני (Join אמורה תמיד להתייחס לקוד
            // שמישהי אחרת יצרה עם Create) - בניגוד ל-buildUri עצמה, שעדיין
            // נופלת ל-DEFAULT_ROOM על קלט ריק (לשמירת תאימות טסטים קיימים) -
            // כאן, בדיאלוג עצמו, פשוט לא מתחברים בכלל ומראים הודעה מקומית.
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

    // מטפל בלחיצה על Play, Create או Join: מתחבר ב-thread נפרד, כי
    // connectBlocking() חוסם - קריאה לו ישירות מה-EDT הייתה מקפיאה את
    // החלון (ואת כל Swing) עד שההתחברות תצליח או תיכשל. מקבלת uriText
    // **מוכן** במקום לבנות אותו כאן - כך שכל נתיבי ההתחברות קוראים
    // לאותה מתודה בדיוק, רק עם URI שונה (buildUri/buildMatchmakingUri/
    // buildCreateRoomUri) שנבנה לפני הקריאה. buttonsToToggle (varargs) -
    // כל כפתורי מסך הבית מושבתים יחד בזמן חיבור (לא רק זה שנלחץ), כדי
    // שלא אפשר לפתוח בטעות שני חיבורים במקביל. username מועבר בנפרד
    // (לא נחלץ מ-uriText בחזרה) כדי ש-NetworkGameWindow יוכל להציג
    // אותו על המסך - הוא כבר "ידוע" כאן לפני שנבנה ה-uri עצמו.
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
                // ROLE_ASSIGNED (כולל ה-gameId בפועל - חשוב במיוחד ל-Create,
                // ר' waitForAssignedGameId) מגיעה כהודעת רשת נפרדת, קצת אחרי
                // שה-handshake עצמו הסתיים - עדיין על thread הרקע, לפני שעוברים ל-EDT.
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

    // ממתינה (על thread הרקע - חסימה כאן בסדר גמור, בדיוק כמו connectBlocking
    // עצמה) עד ש-GameClient.assignedGameId() יתמלא, או עד timeout. לרוב
    // חוזרת כמעט מיד (ROLE_ASSIGNED היא ההודעה הראשונה שהשרת שולח, מיד
    // אחרי onOpen) - אם בכל זאת timeout (תקלת רשת חריגה), מחזירה null,
    // ו-NetworkGameWindow.launch פשוט תציג gameId=null בכותרת (לא קריטי,
    // לא חוסם את המשחק עצמו בכלל).
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

    // מציגה הודעת כישלון בחלון הבית עצמו (label קיים, בלי popup) ומחזירה
    // את כל הכפתורים למצב פעיל - כדי שאפשר יהיה לנסות שוב.
    private static void showFailure(JLabel statusLabel, String message, JButton... buttonsToToggle) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
        setButtonsEnabled(buttonsToToggle, true);
    }

    private static void setButtonsEnabled(JButton[] buttons, boolean enabled) {
        for (JButton button : buttons) {
            button.setEnabled(enabled);
        }
    }
}
