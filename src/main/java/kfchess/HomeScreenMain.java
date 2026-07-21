package kfchess;

import kfchess.net.client.GameClient;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * מסך הבית (שלב 3, "v1") - חלון Swing פשוט שמאפשר להזין room ולהתחבר
 * אליו, לפני שנפתח חלון המשחק עצמו (NetworkGameWindowMain.launch). עדיין
 * אין כאן שום חשבון/authentication אמיתי (זה שלב 4) - "login" בשלב הזה
 * הוא רק בחירת room; מי מקבל WHITE/BLACK/SPECTATOR נקבע בשרת (ClientRole)
 * לפי סדר ההתחברות, לא כאן.
 */
public class HomeScreenMain {

    private static final String SERVER_HOST_AND_PORT = "ws://localhost:8887";
    private static final String DEFAULT_ROOM = "default";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomeScreenMain::buildAndShow);
    }

    // בונה URI מלא לחיבור מתוך שם room גולמי שהמשתמשת הקלידה - room ריק
    // (או רק רווחים) נופל ל-DEFAULT_ROOM, כדי שברירת המחדל תישאר זהה למה
    // ש-NetworkGameWindowMain כבר עושה כשמריצים אותה בלי args בכלל. מופרדת
    // מבניית ה-UI כדי שתהיה ניתנת לבדיקה בלי להרים חלון Swing.
    public static String buildUri(String room) {
        String trimmed = room == null ? "" : room.trim();
        String resolvedRoom = trimmed.isEmpty() ? DEFAULT_ROOM : trimmed;
        return SERVER_HOST_AND_PORT + "/" + resolvedRoom;
    }

    // בונה את חלון הבית עצמו: שדה טקסט ל-room + כפתור Connect + label
    // לסטטוס/שגיאות. רץ על ה-EDT (נקראת רק מתוך main() דרך invokeLater).
    private static void buildAndShow() {
        JFrame frame = new JFrame("KFChess - Home");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField roomField = new JTextField(DEFAULT_ROOM, 15);
        JButton connectButton = new JButton("Connect");
        JLabel statusLabel = new JLabel(" ");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JLabel("Room:"));
        panel.add(roomField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(connectButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(statusLabel);

        connectButton.addActionListener(e ->
                connect(frame, roomField.getText(), connectButton, statusLabel));

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // מטפל בלחיצה על Connect: מתחבר ב-thread נפרד, כי connectBlocking()
    // חוסם - קריאה לו ישירות מה-EDT הייתה מקפיאה את החלון (ואת כל Swing)
    // עד שההתחברות תצליח או תיכשל. תוצאת ההתחברות מדווחת בחזרה ל-EDT דרך
    // SwingUtilities.invokeLater, כי רק שם מותר לגעת ברכיבי Swing
    // (connectButton/statusLabel/homeFrame).
    private static void connect(JFrame homeFrame, String room, JButton connectButton, JLabel statusLabel) {
        connectButton.setEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Connecting...");
        String uriText = buildUri(room);

        new Thread(() -> {
            GameClient client;
            boolean connected;
            try {
                client = new GameClient(new URI(uriText));
                connected = client.connectBlocking();
            } catch (URISyntaxException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> showFailure(connectButton, statusLabel, ex.getMessage()));
                return;
            }

            GameClient finalClient = client;
            boolean finalConnected = connected;
            SwingUtilities.invokeLater(() -> {
                if (finalConnected) {
                    homeFrame.dispose();
                    NetworkGameWindowMain.launch(finalClient);
                } else {
                    showFailure(connectButton, statusLabel, "failed to connect to " + uriText);
                }
            });
        }, "home-screen-connect").start();
    }

    // מציגה הודעת כישלון בחלון הבית עצמו (label קיים, בלי popup) ומחזירה
    // את כפתור Connect למצב פעיל - כדי שאפשר יהיה לתקן את שם ה-room ולנסות שוב.
    private static void showFailure(JButton connectButton, JLabel statusLabel, String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
        connectButton.setEnabled(true);
    }
}
