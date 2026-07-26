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
 * מסך הבית - חלון Swing עם שני נתיבים להתחלת משחק: כפתור "Play"
 * (matchmaking אקראי, ר' buildMatchmakingUri) וכפתור "Room..." (שלב 6 -
 * פותח דיאלוג עם Create/Join/Cancel, ר' showRoomDialog). לפני שלב 6
 * היה כאן גם שדה room חופשי + כפתור Connect - הוסרו: לא היו חלק
 * מהדרישה המקורית בכלל (תוספת-תשתית מסבב קודם), ודיאלוג ה-Room
 * החדש מכסה בדיוק את אותו שימוש (Join = הקלדת ID קיים), רק לפי
 * המפרט המדויק במקום UI מאולתר.
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
    // חייב להיות זהה בדיוק לטוקנים המקבילים ב-MatchmakingResolver/
    // CreateRoomResolver בצד השרת (kfchess.server.server) - כל קצה מגדיר
    // אותם בנפרד, אותו עיקרון בדיוק כמו ש-DEFAULT_ROOM כאן ו-DEFAULT_GAME_ID
    // ב-GameIdResolver כבר מוגדרים בנפרד היום, לא משותפים ע"י מחלקת קבועים אחת.
    private static final String MATCHMAKING_PATH = "_play";
    private static final String CREATE_ROOM_PATH = "_create";
    // כמה זמן (בת-thread רקע, ר' waitForAssignedGameId) לחכות ל-ROLE_ASSIGNED
    // לפני שמוותרים על הצגת gameId מדויקת בכותרת החלון - לא אמור לקחת
    // יותר מכמה מילישניות בפועל (הודעה ראשונה מהשרת אחרי handshake מוצלח),
    // 2 שניות הוא שוליים בטיחות גדול בכוונה.
    private static final long GAME_ID_WAIT_TIMEOUT_MILLIS = 2000;
    private static final long GAME_ID_POLL_INTERVAL_MILLIS = 20;

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

    // בונה URI מלא לחיבור מתוך room id גולמי (Join בדיאלוג ה-Room) - ריק
    // (או רק רווחים) נופל ל-DEFAULT_ROOM, כדי שברירת המחדל תישאר זהה למה
    // ש-NetworkGameWindowMain כבר עושה כשמריצים אותה בלי args בכלל (למשל
    // בדיקות ישירות) - אבל showRoomDialog לא נותנת בפועל ל-Join לקרוא
    // לכאן עם תיבה ריקה (ר' שם), כדי שלא "יתגלגלו" בטעות לחדר default
    // המשותף. מופרדת מבניית ה-UI כדי שתהיה ניתנת לבדיקה בלי להרים חלון Swing.
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
                connect(frame, buildMatchmakingUri(username), statusLabel, playButton, roomButton));
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
            connect(homeFrame, buildCreateRoomUri(username), homeStatusLabel, playButton, roomButton);
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
            connect(homeFrame, buildUri(roomIdField.getText(), username), homeStatusLabel, playButton, roomButton);
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
    // שלא אפשר לפתוח בטעות שני חיבורים במקביל.
    private static void connect(JFrame homeFrame, String uriText, JLabel statusLabel, JButton... buttonsToToggle) {
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
                    NetworkGameWindowMain.launch(finalClient, gameId);
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
    // ו-NetworkGameWindowMain.launch פשוט תציג gameId=null בכותרת (לא קריטי,
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
