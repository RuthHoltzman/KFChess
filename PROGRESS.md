# KFChess Server - Progress & Handoff

מסמך זה נועד לתת לשיחת Claude חדשה (או לרות) תמונת מצב מלאה בלי צורך
לגלול היסטוריית צ'אט ארוכה. בתחילת שיחה חדשה: תני לינק/תגידי "תקראי
את PROGRESS.md בתיקיית הפרויקט ותמשיכי משם".

## המטלה

הוספת צד שרת לפרויקט KFChess (Java, Maven, Java 17) - שחמט בזמן-אמת
("קונג-פו צ'אס") שקיים היום כאפליקציית Swing מקומית (`GameWindowMain`).
לפי מצגת ההוראות (`CTD 26 (Server).pptx.pdf`), חלוקה ל-6 שלבים:

1. **Bus (pub/sub)** - ✅ הושלם, committed.
2. **שרת WebSocket חד-תהליכי בסיסי** - ✅ **הושלם ומאומת מקצה לקצה, committed**.
   כולל גם לקוח Swing מלא במצב רשת (`NetworkGameWindowMain`) - לא רק
   בדיקת קונסולה. אומת בפועל: שני חלונות Swing נפרדים (שני תהליכי
   IntelliJ, "Allow multiple instances") מחוברים בו-זמנית כ-WHITE/BLACK,
   כל אחד יכול להזיז רק את הכלים שלו, שני הלוחות מסונכרים בזמן אמת,
   וסגירת חלון + פתיחת לקוח חדש מצטרפת מחדש לאותו משחק (ר' "אימות ידני"
   למטה).
3. Home screen v1 - login בשל (shell), 2 שחקנים בלבד - ✅ **הושלם ואומת ידנית**
   (ר' למטה). כרגע רק בחירת room (בלי חשבונות/סיסמה - זה שלב 4).
4. חשבונות + ELO (SQLite) - ⬜ **הצעד הבא** (ר' למטה).
5. Matchmaking (Play) + ניתוק/auto-resign - ⬜ לא התחיל.
6. חדרים (Create/Join/Cancel) + לוגים - ⬜ לא התחיל.

## החלטות ארכיטקטורה שנסגרו - כולן ממומשות בפועל

- ספריית WebSocket: **Java-WebSocket 1.6.0** (org.java-websocket) - נוסף ל-pom.xml.
- JSON: **Gson 2.14.0** - נוסף ל-pom.xml.
- פרוטוקול הודעות: אובייקט JSON עם שדה `"type"`. קליק/קפיצה נשלחים כ-
  `{"type":"CLICK","row":..,"col":..}` / `{"type":"JUMP",...}` (מיקום על
  הלוח, לא פיקסלים).
- מבנה השרת: `Map<gameId, GameSession>` ב-`GameServer`, נוצר lazily.
  gameId נלקח מנתיב החיבור עצמו (`ws://host:port/room1` → "room1",
  חיבור לשורש → "default") - כדי לתמוך בכמה משחקים במקביל גם בלי UI
  לחדרים (שלב 6 עדיין לא קיים). מומש ב-`GameIdResolver`.
- **מודל תפקידים**: `ClientRole{WHITE,BLACK,SPECTATOR}` - רחב יותר מ-
  `PieceColor` כי לצופה אין צבע כלל. הראשון שמתחבר ל-GameSession=WHITE,
  השני=BLACK, כל השאר=SPECTATOR. תפקיד נקבע לפי מי **מחובר עכשיו**, לא
  לפי זהות קבועה (אין עדיין חשבונות/התחברות - שלב 4) - כך שסגירת חלון
  ופתיחת לקוח חדש "תופסת" את הצבע שהתפנה.
- **מודל thread-safety ("שרת חד-תהליכי")**: כל שינוי במצב המשחק (GameEngine)
  קורה אך ורק מ-thread אחד - thread הטיק. פקודות שמגיעות מ-threads הרשת
  נכנסות לתור (`GameSession.enqueueCommand`), ו-`tick()` מרוקן ומבצע אותן.
- **snapshot מאוחד**: `SnapshotMessage` כולל `motions`/`jumps`/
  `captureEffects` - אותו מידע שה-UI המקומי כבר מצייר כאנימציה, נלקח
  ישירות מ-`engine.activeMotions()/activeJumps()/recentCaptureEffects()`.
  גם `boardWidthCells`/`boardHeightCells` נשלחים ברשת (כמו ש-`GameSnapshot`
  המקומי כבר מחזיק) - כדי שהלקוח לא יצטרך hard-code של גודל לוח.
- **מזהה יציב לכלי (`Piece.id()`)**: `Piece` קיבל שדה `id` (נקבע פעם
  אחת בבנאי, `AtomicLong` גלובלי) - כדי שהלקוח יוכל לזהות "זה אותו כלי
  שהיה קודם" בין הודעות JSON נפרדות (זהות אובייקט Java הולכת לאיבוד
  בכל פענוח). זו הדרך הסטנדרטית לבעיה הזו (כמו `key` ב-React) - נבחרה
  במפורש **במקום** היוריסטיקת "התאמה לפי מיקום" שהייתה יותר קוד ופחות
  אמינה (שרשראות מהלכים, הכתרה).
- **מינימום DTOs**: `Position`/`Motion`/`CaptureEffect` נשלחים ישירות
  ל-Gson בלי עטיפה. רק `PieceDto`/`JumpDto` נשארו (כי `Piece`/`JumpVisual`
  בכוונה לא יודעים את מיקומם על הלוח).
- **מבנה חבילות (kfchess.net)** - פוצל ל-3:
  - `kfchess.net` - פרוטוקול משותף בלבד (`ClientCommand`, `ClientCommandType`,
    `SnapshotMessage`, `PieceDto`, `JumpDto`, `ClientRole`, `RoleAssignedMessage`,
    `ErrorMessage`).
  - `kfchess.net.server` - `GameServer`, `GameSession`, `ServerMain`, `GameIdResolver`.
  - `kfchess.net.client` - `GameClient`, `ClientMain`, `IncomingMessageSummary`,
    `IncomingSnapshot`, `ClientSnapshotReconstructor`.
- **`ClientSnapshotReconstructor`** (בצד הלקוח) - הופך `IncomingSnapshot`
  (JSON מפוענח) ל-`Board`+`Motion`+`JumpVisual` אמיתיים, עם **אותו אובייקט
  Piece בדיוק** בין הודעות עוקבות (לפי `piece.id()`) - כדי ש-`SnapshotFactory`
  הקיים (זהה למשחק המקומי, לא שונה בכלל) יעבוד כמו שהוא, **כולל** אנימציית
  שעון-החול (SHORT_REST/LONG_REST) שתלויה בזיהוי "זה אותו כלי". יש לו
  state פנימי (`knownPieces`) בכוונה - חובה לזכור בין הודעות.
- **`BoardLayoutCalculator`** (`kfchess.view.layout`) - חישוב הגיאומטריה
  של הלוח על המסך, משותף בין `GameWindowMain` (מקומי) ל-`NetworkGameWindowMain`
  (רשת) - היה קוד פרטי משוכפל בפוטנציה, עכשיו מקום אחד.
- **`NetworkGameWindowMain`** - חלון Swing במצב רשת, **בלי GameEngine מקומי
  בכלל**. משתמש ב-**polling** (Timer של Swing קורא ל-`GameClient.latestMessage()`
  בכל טיק) ולא callback ישיר מ-thread הרשת - כדי שציור Swing תמיד יקרה
  בבטחה על ה-EDT, בדיוק כמו שהחלון המקומי כבר עושה עם ה-Timer שלו. קליק/
  קליק-ימני רק שולחים CLICK/JUMP לשרת - בלי שום בדיקת-תפקיד כפולה בצד
  הלקוח (השרת כבר אוכף הכל, כולל דחיית קליק של צופה).

## מה שכבר קיים, מומש ו-committed

### Bus (`kfchess.bus`)
`EventBus` גנרי, 4 סוגי אירועים: `ScoreUpdatedEvent`, `MoveLoggedEvent`,
`SoundEvent`, `GameLifecycleEvent`.

### `kfchess.engine`
`GameEngine`, `PieceTimers`, `MoveHistory`, `NetworkActions`, `snapshot/`
(`SnapshotFactory` ועוד - משותף לחלוטין בין משחק מקומי לרשת).

### `kfchess.net` + `kfchess.net.server` + `kfchess.net.client`
ר' "מבנה חבילות" למעלה לרשימת הקבצים המלאה. כל הקבצים מכוסים בטסטים
ב-`src/test/java/texttests/` (טסט לכל DTO/מחלקת לוגיקה - כולל
`ClientSnapshotReconstructorTest` שבודק שימור זהות בין הודעות במפורש).

### `kfchess.view.layout.BoardLayoutCalculator`
גיאומטריית הלוח, משותפת בין שני החלונות.

### `kfchess.GameWindowMain` (מקומי) ו-`kfchess.NetworkGameWindowMain` (רשת)
שני נקודות כניסה עם Swing - חולקות את כל שכבת הציור (`SnapshotFactory`,
`GameSceneView`, `BoardView`, `BoardLayoutCalculator`) בלי שכפול.
`NetworkGameWindowMain.main()` נשארה נקודת כניסה ישירה לבדיקות (מתחברת
ל-URI קבוע/מ-`args[0]`), אבל כל הלוגיקה של "פתיחת חלון המשחק בהינתן
`GameClient` שכבר מחובר" הוצאה למתודה `public static launch(GameClient)`
נפרדת - כדי ש-`HomeScreenMain` תוכל להשתמש באותו קוד בדיוק בלי לשכפל
אותו.

### `kfchess.HomeScreenMain` (מסך בית, שלב 3 v1)
חלון Swing נפרד: שדה room (ברירת מחדל `"default"`) + כפתור Connect.
מתחבר ב-thread רקע (כי `connectBlocking()` חוסם, ולא רוצים להקפיא את
ה-EDT) ל-`ws://localhost:8887/<room>`; הצליח → סוגר את עצמו וקורא
ל-`NetworkGameWindowMain.launch(client)`; נכשל → מציג שגיאה ב-label
בתוך אותו חלון (בלי popup) ומאפשר לנסות שוב. `buildUri(room)` היא
פונקציה טהורה ונפרדת (trim + נפילה ל-`"default"` על קלט ריק/רווחים/
`null`) - נבדקת ישירות ב-`HomeScreenMainTest` בלי להרים UI. עדיין אין
כאן חשבונות/סיסמה בכלל (שלב 4) - "login" בשלב הזה הוא רק בחירת room;
מי מקבל WHITE/BLACK/SPECTATOR עדיין נקבע בשרת לפי סדר התחברות
(`ClientRole`, ר' למעלה), לא כאן.

## איך להריץ ולבדוק (IntelliJ)

1. Run על `kfchess.net.server.ServerMain` - אמורה להיכתב שורה
   `GameServer started on port 8887`.
2. Run על `kfchess.HomeScreenMain` - נפתח מסך בית, מזינים room (או
   משאירים `default`) ולוחצים Connect - נכנס כ-WHITE. (אפשר גם Run
   ישירות על `kfchess.NetworkGameWindowMain`, בלי מסך בית, לבדיקות מהירות.)
3. **לשני שחקנים בו-זמנית**: בקונפיגורציית `HomeScreenMain` (או
   `NetworkGameWindowMain`) (Edit Configurations → Modify options →
   **Allow multiple instances**), ואז Run עליה **שוב** בלי לעצור את
   הריצה הראשונה - מתחברים לאותו room (`default` אם לא שינו) כדי
   להיכנס לאותה `GameSession`, נכנס כ-BLACK.
4. בדיקת פרוטוקול גולמי (כמו קודם): מקונסולת דפדפן (F12) עם `WebSocket`
   ישיר, או `kfchess.net.client.ClientMain` (לקוח קונסולה).

## אימות ידני שבוצע בפועל

- `mvn clean test` - **ירוק לגמרי, 43/43** (אחרי הסרת 4 טסטים ב-
  `BoardParserTest` שציפו ל-`null` בעוד הקוד זורק חריגה בכוונה - אי-
  התאמה ישנה שתועדה כאן בעבר, טופלה סופית).
- שני חלונות `NetworkGameWindowMain` בו-זמנית (WHITE+BLACK): כל אחד
  יכול לבחור/להזיז רק את הכלים שלו (השרת דוחה קליק על כלי לא-שלו בשקט -
  זו התנהגות נכונה, לא באג), שני הלוחות מסונכרים בזמן אמת.
- סגירת חלון לקוח ופתיחת חדש: מצטרף מחדש לאותה `GameSession` שכבר
  קיימת (המצב חי על השרת, לא בלקוח) - **בכוונה**, לא באג. **שימו לב**:
  עדיין **אין** טיפול בניתוק (auto-resign/timeout, שלב 5) - אם צד מתנתק,
  המשחק פשוט ממתין, אף אחד לא מפסיד.
- `HomeScreenMain` **אומת ידנית ע"י רות ועובד** - הזנת room, Connect,
  ומעבר לחלון המשחק (`NetworkGameWindowMain.launch`).

## הצעד הבא

**שלב 4**: חשבונות + ELO (SQLite) - כרגע אין שום authentication אמיתי;
`HomeScreenMain` (שלב 3) רק בוחר room, ותפקיד WHITE/BLACK/SPECTATOR
נקבע לפי סדר התחברות בלבד (`ClientRole`). הצעד הבא הוא הוספת חשבונות
אמיתיים (username+password, ככל הנראה עם SQLite) ודירוג ELO לכל
שחקן/ת - כולל החלטה איך זה משתלב עם מסך הבית הקיים (למשל: שדה
username/password לפני שדה ה-room, או מסך login נפרד לפניו).

## סגנון עבודה מוסכם עם רות

- **לשאול ולהסביר לפני כל קובץ/מחלקה/פונקציה חדשה** - לא רק לפני
  שינויים ארכיטקטוניים גדולים. שורת הערה שמסבירה *למה* (לא רק *מה*)
  לפני כל פונקציה בקוד עצמו.
- לוודא שהגישה המוצעת היא "הדרך המומלצת" (סטנדרטית, לא היוריסטיקה/
  hard-code) לפני שממשיכים - למשל: `Piece.id()` יציב במקום התאמה לפי
  מיקום, `boardWidthCells`/`boardHeightCells` ברשת במקום hard-code 8x8.
- לערוך את הקבצים ישירות (לא רק להציג קוד בצ'אט).
- **לכתוב טסט לכל פונקציה/לוגיקה משמעותית** (`src/test/java/texttests/`).
- **ארגון קוד**: לחלק לחבילות-משנה הגיוניות (`net.client`/`net.server`,
  `view.layout`) במקום הכל שטוח באותה חבילה, ולא לשכפל קוד בין קבצים -
  אם משהו משותף לשני מקומות, מוציאים למחלקה משותפת.
- **אחרי כל שינוי מוכן: להציע פקודת commit מדויקת (git add + git commit -m)
  אבל לא להריץ אותה בעצמי** - רות תמיד מריצה. `git status`/`git diff`
  מותר לי להריץ (קריאה בלבד) כדי לוודא שה-commit המוצע מדויק ולא כולל
  קבצים לא-קשורים.
- אחרי כל שינוי מבני: לבדוק איזון סוגריים + חיפוש הפניות ישנות שנשברו
  (grep), לפני שמבקשים ממנה להריץ `mvn compile`/`mvn test` (אין
  JDK/Maven מלאים בסביבת הכלים שלי - יש רק JRE 11, אין javac).
- דגש חזק על קוד מסודר/לא-ארוך-מדי - מחלקות קטנות וממוקדות
  (single responsibility).
