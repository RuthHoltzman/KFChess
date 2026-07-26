package kfchess;

import kfchess.account.Account;
import kfchess.server.client.GameClient;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * מסך הבית (שלב 3, "v1") - חלון Swing פשוט שמאפשר להזין room ולהתחבר
 * אליו, לפני שנפתח חלון המשחק עצמו (NetworkGameWindowMain.launch).
 * <p>
 * מאז שלב 4 יש authentication אמיתי לפני המסך הזה (kfchess.LoginScreenMain,
 * המיין היחיד להרצת הלקוח) - הוא קורא ל-launch(Account) עם ה-Account
 * המחובר, ומוצג כאן כתווית "Logged in as". מאז שלב 4 Part B, ה-username
 * גם נשלח בפועל לשרת (כ-query parameter על ה-URI, ר' buildUri) - כדי
 * ש-GameSession ידע למי לעדכן ELO בסוף המשחק (עדיין **לא** משפיע על מי
 * מקבל WHITE/BLACK/SPECTATOR - זה עדיין לפי סדר התחברות, ר' ClientRole).
 * אין כאן main() עצמאי בכוונה - ר' LoginScreenMain.
 */
public class HomeScreenMain {

    private static final String SERVER_HOST_AND_PORT = "ws://localhost:8887";
    private static final String DEFAULT_ROOM = "default";
    // חייב להיות זהה בדיוק לטוקן שקבוע ב-MatchmakingResolver בצד השרת
    // (kfchess.server.server) - שני הקצוות מגדירים אותו בנפרד, אותו עיקרון
    // בדיוק כמו ש-DEFAULT_ROOM כאן ו-DEFAULT_GAME_ID ב-GameIdResolver כבר
    // מוגדרים בנפרד היום, לא משותפים ע"י מחלקת קבועים אחת.
    private static final String MATCHMAKING_PATH = "_play";

    // נקודת הכניסה היחידה למסך הזה - נקראת מ-LoginScreenMain אחרי
    // login/register מוצלח, עם ה-Account שהתקבל.
    public static void launch(Account account) {
        SwingUtilities.invokeLater(() -> buildAndShow(account));
    }

    // חתימה ישנה, בלי username - נשארת כדי ש-HomeScreenMainTest הקיים
    // ימשיך לעבוד בלי שינוי; שקולה ל-buildUri(room, null) (בלי query
    // string בכלל - זה בדיוק מה שקורה כשמריצים HomeScreenMain בלי login,
    // למשל NetworkGameWindowMain.main() לבדיקות ישירות).
    public static String buildUri(String room) {
        return buildUri(room, null);
    }

    // בונה URI מלא לחיבור מתוך שם room גולמי שהמשתמשת הקלידה - room ריק
    // (או רק רווחים) נופל ל-DEFAULT_ROOM, כדי שברירת המחדל תישאר זהה למה
    // ש-NetworkGameWindowMain כבר עושה כשמריצים אותה בלי args בכלל. מופרדת
    // מבניית ה-UI כדי שתהיה ניתנת לבדיקה בלי להרים חלון Swing.
    // username (משלב 4 Part B): אם יש (לא null/ריק) מתווסף כ-query
    // parameter מקודד-URL על אותו URI, כדי ש-GameServer/UsernameResolver
    // ידעו לשייך את החיבור לחשבון - בלי username בכלל (login-פחות, למשל
    // בדיקות ישירות) מתקבל בדיוק אותו URI כמו קודם, בלי שינוי.
    public static String buildUri(String room, String username) {
        String trimmed = room == null ? "" : room.trim();
        String resolvedRoom = trimmed.isEmpty() ? DEFAULT_ROOM : trimmed;
        String base = SERVER_HOST_AND_PORT + "/" + resolvedRoom;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    // בונה URI לבקשת matchmaking (כפתור "Skip", שלב 5 חלק 2) - נתיב שמור
    // (MATCHMAKING_PATH) במקום שם room שהמשתמשת הקלידה; אותה שיטת קידוד
    // username בדיוק כמו buildUri, בכוונה בלי לשכפל את לוגיקת ה-URLEncoder.
    public static String buildMatchmakingUri(String username) {
        String base = SERVER_HOST_AND_PORT + "/" + MATCHMAKING_PATH;
        if (username == null || username.isBlank()) {
            return base;
        }
        return base + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    // בונה את חלון הבית עצמו: שדה טקסט ל-room + כפתור Connect (חדר ספציפי,
    // ר' buildUri) + כפתור Skip חדש (matchmaking אקראי, ר' buildMatchmakingUri,
    // מתעלם משדה ה-room לגמרי) + label לסטטוס/שגיאות, ובנוסף שורת "Logged in
    // as" אם הגיעה לכאן דרך launch(Account) אחרי login (account != null).
    // רץ על ה-EDT (נקראת רק מתוך main()/launch() דרך invokeLater).
    private static void buildAndShow(Account account) {
        JFrame frame = new JFrame("KFChess - Home");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField roomField = new JTextField(DEFAULT_ROOM, 15);
        JButton connectButton = new JButton("Connect");
        JButton skipButton = new JButton("Skip");
        JLabel statusLabel = new JLabel(" ");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        if (account != null) {
            panel.add(new JLabel("Logged in as: " + account.username() + " (ELO " + account.elo() + ")"));
            panel.add(Box.createVerticalStrut(8));
        }
        panel.add(new JLabel("Room:"));
        panel.add(roomField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(connectButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(skipButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(statusLabel);

        String username = account == null ? null : account.username();
        connectButton.addActionListener(e ->
                connect(frame, buildUri(roomField.getText(), username), connectButton, skipButton, statusLabel));
        skipButton.addActionListener(e ->
                connect(frame, buildMatchmakingUri(username), connectButton, skipButton, statusLabel));

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // מטפל בלחיצה על Connect *או* Skip: מתחבר ב-thread נפרד, כי connectBlocking()
    // חוסם - קריאה לו ישירות מה-EDT הייתה מקפיאה את החלון (ואת כל Swing)
    // עד שההתחברות תצליח או תיכשל. מקבלת uriText **מוכן** במקום לבנות אותו
    // כאן - כך ששני הכפתורים קוראים לאותה מתודה בדיוק, רק עם URI שונה
    // (buildUri/buildMatchmakingUri) שנבנה לפני הקריאה; ומשביתה את *שני*
    // הכפתורים (לא רק זה שנלחץ) כדי שלא אפשר ללחוץ על השני תוך כדי חיבור
    // ולפתוח בטעות שני חיבורים. תוצאת ההתחברות מדווחת בחזרה ל-EDT דרך
    // SwingUtilities.invokeLater, כי רק שם מותר לגעת ברכיבי Swing.
    private static void connect(JFrame homeFrame, String uriText,
                                 JButton connectButton, JButton skipButton, JLabel statusLabel) {
        connectButton.setEnabled(false);
        skipButton.setEnabled(false);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setText("Connecting...");

        new Thread(() -> {
            GameClient client;
            boolean connected;
            try {
                client = new GameClient(new URI(uriText));
                connected = client.connectBlocking();
            } catch (URISyntaxException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> showFailure(connectButton, skipButton, statusLabel, ex.getMessage()));
                return;
            }

            GameClient finalClient = client;
            boolean finalConnected = connected;
            SwingUtilities.invokeLater(() -> {
                if (finalConnected) {
                    homeFrame.dispose();
                    NetworkGameWindowMain.launch(finalClient);
                } else {
                    showFailure(connectButton, skipButton, statusLabel, "failed to connect to " + uriText);
                }
            });
        }, "home-screen-connect").start();
    }

    // מציגה הודעת כישלון בחלון הבית עצמו (label קיים, בלי popup) ומחזירה
    // את שני הכפתורים למצב פעיל - כדי שאפשר יהיה לתקן את שם ה-room/ללחוץ
    // Skip ולנסות שוב.
    private static void showFailure(JButton connectButton, JButton skipButton, JLabel statusLabel, String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
        connectButton.setEnabled(true);
        skipButton.setEnabled(true);
    }
}
