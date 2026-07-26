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
4. חשבונות + ELO (SQLite) - ✅ **הושלם ואומת ידנית ע"י רות**. Part A:
   login/register מקומי. Part B: ה-username נשלח בפועל לשרת (query param
   על ה-URI) ו-ELO מתעדכן אוטומטית בסוף כל משחק (`EloCalculator`, K=32).
   בנוסף (מעבר לדרישות השלב): **פיצ'ר Restart הדדי** - ✅ הושלם ואומת
   ידנית (ר' סעיף ייעודי למטה).
5. Matchmaking ("Play"/Skip) + ניתוק/auto-resign - ✅ **auto-resign הושלם
   ואומת לגמרי, committed.** כפתור "Skip" שונה שם ל-**"Play"**. **תוקן
   ואומת**: באג שרות מצאה - Play היה "גונב" חדר פרטי שנוצר דרך Create -
   **אושר ע"י רות**. **תיקון מלא לפי המפרט המקורי** (ELO ±100, timeout
   של דקה, הודעת "לא נמצא") - **ה-timeout אומת ועובד** (רות הריצה,
   popup נפתח אחרי דקה והחלון נסגר). התאמת ה-ELO עצמה עדיין לא אומתה
   במפורש (דורש שני חשבונות עם ELO מכוון) - לא חוסם.
6. חדרים (Create/Join/Cancel) + לוגים - ✅ **שני החלקים ממומשים, `mvn test`
   ירוק (141/141), ואומתו ידנית ע"י רות** ("הרצתי טסטים הרצתי חלוניות
   הכל עובד"). Create/Join/Cancel, התיקון ל-Play/חדרים פרטיים, חסימת
   מהלכים בזמן המתנה, הבאנר הכחול, והלוגים בצד שרת+לקוח - כולם עובדים.

## הערה חשובה - קובץ ההוראות המקורי (PDF) והפער הידוע משלב 5

רות סיפקה את `CTD 26 (Server).pptx (1).pdf` (לא היה קודם בתיקיית
הפרויקט - חולץ טקסט ממנו, לא נשמר קובץ). **הציטוט המדויק לשלב 5**:

> Add "Play" Button: Finds the other player with ELO in range of ±100
> that also seeks for a game. If doesn't find - waits for 1 min, if
> can't find - pops up a message that can't find.
> If player disconnected - auto-resign after 20 sec. Make a "count down" on the screen.

**auto-resign + countdown תואמים בדיוק** למה שכבר מומש (שלב 5 חלק 1).
אבל כפתור ה-"Skip" (שלב 5 חלק 2, כפי שמומש) **סוטה מהמפרט**: לא בודק
טווח ELO ±100 בכלל (מתאים לכל מי שממתין/ה), ולא מגביל את ההמתנה לדקה
עם הודעת "לא נמצא" (ממתין ללא הגבלה, פותח משחק חדש מיד אם אין מתאים).
**רות ביקשה במפורש לדחות את התיקון** ולהתקדם לשלב 6 קודם - זה **עדיין
פתוח וצריך לחזור אליו**.

**הציטוט המדויק לשלב 6** (המקור לתכנון החדרים בסעיף הייעודי למטה):

> At Home screen: Button: Room → Open a windows message with text box
> and buttons: Create / Join / Cancel. Create: Generated a new room id,
> and writes it on top of the screen. Join: Enters the room whose ID
> you typed in the text box. The room ID is again written on the top
> of the screen. Inside a room: the second person that joins the room
> is the Black player of the game. The following people who join are viewers.
> Store logs on both server and client side, for all of the client/server activity.

## הערה טכנית חשובה - שינוי שם חבילה (לא נעשה על ידי Claude)

רות שינתה (refactor דרך IntelliJ, "Rename Package", בין סבב "שלב 5 חלק 1"
לסבב "שלב 5 חלק 2") את שם החבילה `kfchess.net` ל-`kfchess.server` בכל
הפרויקט - `kfchess.net.server`→`kfchess.server.server`,
`kfchess.net.client`→`kfchess.server.client`, ו-`kfchess.net.*` (ה-DTOs
עצמם - `ClientCommand`/`SnapshotMessage`/`ClientRole`/וכו') →`kfchess.server.*`
ישירות. **בוצע commit נפרד לשינוי השם עצמו** (לפני commit ה-matchmaking).
**חשוב לכל שיחת AI עתידית**: כל התיעוד למעלה בקובץ הזה (מלפני שני
הסבבים האלה) עדיין מזכיר `kfchess.net.*` בטקסט - זה נכון *היסטורית* (זה
היה שם החבילה כשזה נכתב), אבל **הנתיבים בפועל בקוד היום הם `kfchess.server.*`**.

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
- **מבנה חבילות (kfchess.server)** - פוצל ל-3:
  - `kfchess.server` - פרוטוקול משותף בלבד (`ClientCommand`, `ClientCommandType`,
    `SnapshotMessage`, `PieceDto`, `JumpDto`, `ClientRole`, `RoleAssignedMessage`,
    `ErrorMessage`).
  - `kfchess.server.server` - `GameServer`, `GameSession`, `ServerMain`, `GameIdResolver`.
  - `kfchess.server.client` - `GameClient`, `IncomingMessageSummary`,
    `IncomingSnapshot`, `ClientSnapshotReconstructor` (`ClientMain` היה כאן
    גם הוא - **הוסר** בניקוי המיינים, ר' למטה).
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

### `kfchess.server` + `kfchess.server.server` + `kfchess.server.client`
ר' "מבנה חבילות" למעלה לרשימת הקבצים המלאה. כל הקבצים מכוסים בטסטים
ב-`src/test/java/texttests/` (טסט לכל DTO/מחלקת לוגיקה - כולל
`ClientSnapshotReconstructorTest` שבודק שימור זהות בין הודעות במפורש).

### `kfchess.view.layout.BoardLayoutCalculator`
גיאומטריית הלוח - במקור נכתבה כדי לשתף קוד בין חלון המשחק המקומי
(`GameWindowMain`, **הוסר**, ר' "ניקוי המיינים" למטה) לחלון הרשת, נשארה
מחלקה עצמאית ונבדקת גם אחריו.

### `kfchess.NetworkGameWindowMain` (חלון המשחק, רשת בלבד)
נקודת הכניסה היחידה ל"חלון המשחק עצמו" - חולקת את כל שכבת הציור
(`SnapshotFactory`, `GameSceneView`, `BoardView`, `BoardLayoutCalculator`)
עם מה שהיה קודם קוד מקומי (הוסר). **אין לה `main()` עצמאי** (הוסר, ר'
"ניקוי המיינים") - נפתחת אך ורק דרך `public static launch(GameClient)`,
שנקראת מ-`HomeScreenMain` אחרי חיבור מוצלח. טיפול הקליק עצמו **הוצא**
ל-`NetworkClickHandler` (ר' מיד למטה) - `NetworkGameWindowMain` רק
מחשב את ה-`BoardLayout` הנוכחי (תלוי בגודל חלון + מידות לוח חיים, לא
ניתן להוציא) ומעביר אותו הלאה.

### `kfchess.server.client.NetworkClickHandler` (הוצא מ-NetworkGameWindowMain)
רות שאלה "האם הדרך הנוכחית הגיונית" אחרי שהתברר ש-`kfchess.input.GameController`
הישן (טיפול קליק במשחק המקומי, שהוסר) הפך לקוד מת - כי הלוגיקה שם
קראה ישירות ל-`GameEngine`, וזה לא מתאים למצב רשת (צריך *לשלוח* לשרת,
לא לקרוא למנוע). התשובה: מטרת ה-refactor הזה **לא** להחזיר את
`GameController` אלא לתת ל"טיפול קליק ברשת" בית משלו, באותה רוח -
בדיוק כמו ש-`NetworkActions` (kfchess.engine) כבר עושה בצד השרת
(מחלקה קטנה שרק מנתבת קליק, בלי לוגיקת משחק).
- **`resolvePosition(pixelX, pixelY, layout)`** - טהורה לגמרי (בלי
  Swing/רשת) - ממירה פיקסל למיקום לוגי או `Optional.empty()` אם מחוץ
  ללוח. זו בדיוק הלוגיקה שההערה הישנה ב-`BoardLayoutCalculator` מזהירה
  עליה כמקור לשני באגים קודמים ("קליק לא במקום") - עכשיו **נבדקת ישירות**
  (`NetworkClickHandlerTest`, כולל כל 4 כיווני "מחוץ ללוח").
- **`handle(pixelX, pixelY, layout, gameOver, isJump)`** - שכבה דקה:
  בודקת gameOver, קוראת ל-resolvePosition, שולחת CLICK/JUMP ל-`GameClient`
  אם יש תוצאה. לא נבדקת ישירות בהצלחה (מצריך `GameClient` מחובר בפועל) -
  אותה גישה כמו `HomeScreenMain.connect()`/`LoginScreenMain.handleLogin()`
  שגם הן לא נבדקות ישירות, רק החלקים הטהורים סביבן. כן נבדקים בטסטים
  שני הענפים ש"בורחים" לפני שנוגעים ב-client (gameOver / קליק מחוץ ללוח,
  עם `client=null` - מוכיח שאין נגיעה בו).

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

### `kfchess.account` (שלב 4, Part A) ו-`kfchess.LoginScreenMain`

חשבונות מקומיים ב-SQLite (`kfchess.db`, נוצר אוטומטית) - **עדיין בלי
חיבור לפרוטוקול הרשת/ELO בפועל**, ר' "הצעד הבא" למטה.

- **`Account`** - record(username, elo) - כל מה שמותר לצאת מ-repository
  החוצה (בלי password hash בכלל).
- **`PasswordHasher`** - עטיפה דקה סביב **jBCrypt** (נבחר על פני PBKDF2
  מובנה - salt מנוהל אוטומטית בתוך ה-hash, פחות קוד תשתית מסביב).
- **`AccountRepository`** (ממשק) + **`SqliteAccountRepository`** (מימוש) -
  `register`/`login`. `register` על username קיים זורק
  **`UsernameTakenException`** (checked בכוונה - תרחיש עסקי צפוי, לא
  שגיאת תכנות) - מזוהה לפי קוד השגיאה של SQLite (`SQLITE_CONSTRAINT`),
  לא לפי SELECT-then-INSERT (חוסך race condition). `login` לא מבדיל
  כלפי חוץ בין "username לא קיים" ל"סיסמה שגויה" (הגנה מפני user
  enumeration) - שניהם `Optional.empty()`. elo התחלתי: **1200**.
- **`LoginScreenMain`** - מסך Swing חדש, **לפני** `HomeScreenMain`, וגם
  **המיין היחיד להרצת הלקוח** (ר' "ניקוי המיינים" למטה): username+password,
  כפתורי **Login** ו-**Register נפרדים** (לא auto-register - החלטה
  מפורשת של רות). הצלחה → `HomeScreenMain.launch(account)`.
- **`HomeScreenMain`** קיבל שינוי: `launch(Account)` (במקום `buildAndShow()`
  חסר-פרמטרים) - `account` מוצג רק כתווית "Logged in as" בראש המסך;
  **עדיין לא** משפיע על WHITE/BLACK/SPECTATOR (זה עדיין לפי סדר התחברות
  בשרת, `ClientRole`). **אין לה יותר `main()` עצמאי** - ר' "ניקוי המיינים".
- טסטים: `PasswordHasherTest`, `SqliteAccountRepositoryTest` (על קובץ
  DB זמני, `@TempDir`), `LoginScreenMainTest` (טסט ל-`validate()` הטהורה,
  בלי להרים UI - נדרש להפוך אותה מ-package-private ל-`public` כדי
  שתהיה נגישה מ-`texttests`).

### ניקוי המיינים (אותה שיחה, אחרי בקשה מפורשת של רות)

לפני זה היו יותר מדי נקודות `main()` בפרויקט ("למה יש כל כך הרבה
main??"). המצב הסופי שסוכם ובוצע:

- **`kfchess.server.server.ServerMain`** - השרת, תהליך נפרד (תמיד היה ככה -
  זו המהות של client-server, לא "בלגן").
- **`kfchess.LoginScreenMain`** - **המיין היחיד ללקוח/למשחק בפועל**.
  Login/Register → `HomeScreenMain` (room) → `NetworkGameWindowMain`
  (המשחק) - שרשרת אחת, בלי לבחור בין כמה נקודות כניסה.
- **`kfchess.Main`** - **לא נגעתי בו בכלל** (המשחק קונסולה מקורי, לפני
  הרשת) - רות ציינה שהוא צריך להישאר בשביל טסטים/מטלה קודמת.
- **הוסרו לגמרי**: `kfchess.GameWindowMain` (משחק Swing מקומי, בלי שרת)
  ו-`kfchess.server.client.ClientMain` (לקוח קונסולה גולמי לבדיקת פרוטוקול) -
  שום קוד אחר לא היה תלוי בהם בפועל (רק הערות תיעוד, שעודכנו).
- **הוסר `main()` העצמאי** מ-`NetworkGameWindowMain` ומ-`HomeScreenMain`
  (המחלקות עצמן ומתודות ה-`launch()`/`launch(Account)` נשארו - הן חלק
  מהשרשרת האמיתית).
- אגב הניקוי נמצא ונמחק גם קוד מת ישיר (לא התבקש, אבל תוצאה ישירה של
  המחיקות): `GameClient.printLatestSnapshot()` (השתמשה בו רק `ClientMain`
  שנמחק).
- **`kfchess.input.GameController` נמחק** (בקשה מפורשת של רות, "תמחק את
  מה שלא צריך") - היה קוד מת אמיתי: שום קובץ לא היה תלוי בו (רק
  `GameWindowMain` שהוסר קרא לו - קרא ל-`engine` ישירות, מה שלא מתאים
  למצב רשת ממילא, ר' `NetworkClickHandler` למעלה שעושה את אותו תפקיד
  בשביל הרשת).
- **תיקון טעות שלי**: קודם חשבתי בטעות ש-`GameSceneView.restartButtonBounds()`
  גם היא קוד מת - **לא נכון**, בדיקה יסודית יותר (קריאת כל הקובץ, לא רק
  grep חלקי) הראתה ש-`drawGameOverOverlay()` **כן** קוראת לה בפועל, כדי
  לצייר את כפתור ה-Restart כשהמשחק נגמר (גם במצב רשת). היא נשארה בקוד
  בלי שינוי - זה פיצ'ר חלקי אמיתי (ציור כן, טיפול-קליק על הכפתור עדיין
  לא, ר' "הצעד הבא"), לא קוד מת.

### שלב 4, Part B - חיבור username לפרוטוקול הרשת + עדכון ELO בפועל

אחרי ש-Part A (חשבונות מקומיים) עבד, רות ביקשה במפורש להמשיך פונקציונליות
במקום עיצוב - זה מה שנוסף:

- **איך username "נוסע" מהלקוח לשרת**: כ-query parameter על ה-URI של
  חיבור ה-WebSocket עצמו (כמו שה-room כבר עשה) - `ws://host:port/<room>?username=<username>`,
  מקודד-URL (`URLEncoder`/`URLDecoder`, לא רק `+` בין מילים). `HomeScreenMain.buildUri`
  קיבל overload חדש `buildUri(room, username)` - הישן `buildUri(room)`
  נשאר (שקול ל-`username=null`, כדי ש-`HomeScreenMainTest` הקיים ימשיך
  לעבוד בלי שינוי).
- **באג סמוי שנמצא ותוקן**: `GameIdResolver` לא חתך query string בכלל -
  לפני שהוספתי `?username=`, זה לא שם לב כי לא היה אף פעם query string
  על ה-URI. תוקן (חותך הכל אחרי `?` לפני חישוב ה-gameId) + טסטים חדשים.
- **`kfchess.server.server.UsernameResolver`** (חדש, מקביל ל-`GameIdResolver`
  אבל מחלקה נפרדת - זה query parameter נפרד לגמרי מנתיב ה-room) - מחלץ
  את ה-username מה-query string, `Optional.empty()` אם אין (חיבור בלי
  login בכלל, למשל בדיקת פרוטוקול גולמי - עדיין נתמך, בלי שיוך חשבון).
- **`GameSession`**: `Map<WebSocket, ClientRole>` הפך ל-`Map<WebSocket, ConnectedPlayer>`
  (`record ConnectedPlayer(ClientRole role, String username)` פנימי) -
  `connections()` הציבורית עדיין מחזירה `Map<WebSocket, ClientRole>` (לא
  משנה את מה ש-`GameServer`/הטסטים הקיימים רואים). **בנאים/מתודות ישנים
  נשארו כ-overload** (`GameSession()`, `assignRole(connection)`) בדיוק
  כדי ש-`GameSessionTest` הקיים (8 טסטים) ימשיך לעבוד בלי שום שינוי.
  נרשם ל-`GameLifecycleEvent(ENDED)` (קיים מראש ב-bus, מתפרסם **פעם
  אחת בדיוק** ברגע לכידת המלך - לא בכל tick) ומעדכן ELO לשני הצדדים.
  מדלג בשקט (בלי שגיאה) אם: אין `AccountRepository` בכלל (הבנאי הישן),
  צד אחד לא היה מזוהה, או ששני הצדדים אותו username (בדיקה עצמית - אי
  אפשר לדרג נגד עצמך).
- **`GameServer`**: מחזיק `AccountRepository` אחד משותף (`SqliteAccountRepository`
  על `kfchess.db` - **אותו קובץ בדיוק** ש-`LoginScreenMain` משתמש בו,
  כי אלה אותם חשבונות), מעביר אותו לכל `GameSession` חדש + שולף username
  מ-`UsernameResolver` ומעביר ל-`assignRole`.
- **`kfchess.account.EloCalculator`** (חדש, טהור לגמרי - בלי SQLite/רשת) -
  נוסחת ELO הסטנדרטית, K=32 (רות אישרה במפורש, לעומת 16 השמרני יותר).
  `applyResult(winnerElo, loserElo)` מחזיר `int[]{newWinner, newLoser}`.
  נבדק עם 3 דוגמאות מספריות מוכרות (דירוגים שווים, favorite מנצח, upset) -
  חושבו ואומתו גם ב-Python בנפרד לפני כתיבת הטסט, לא רק "נראה הגיוני".
- **`SqliteAccountRepository.DEFAULT_DB_FILE`** - קבוע ציבורי חדש
  (`"kfchess.db"`) - במקום מחרוזת משוכפלת בין `LoginScreenMain` ל-`GameServer`.
  גם `currentElo(username)`/`updateElo(username, newElo)` נוספו ל-
  `AccountRepository`/`SqliteAccountRepository` + טסטים.
- **מה שעדיין לא נפתר (מודע, לא שכחתי)**: "מי היה WHITE/BLACK" ל-ELO
  נקבע לפי מי שמחזיק את התפקיד *ברגע שהמשחק נגמר* - לא פותר ניתוק-
  והחלפה באמצע משחק (זה שלב 5, `auto-resign`, עדיין לא קיים).

### פיצ'ר Restart - שני הצדדים חייבים ללחוץ (אחרי שלב 4 Part B)

רות בדקה ידנית את שלב 4 Part B, הגיעה ל-game over, הציון (ELO) התעדכן
נכון - אבל גילתה שכפתור ה-Restart **מצויר** (`restartButtonBounds()`,
ר' "ניקוי המיינים" למעלה) אבל **לא מחובר לשום קליק בפועל**. הוחלט (2
שאלות ל-רות): (1) **שני הצדדים** חייבים ללחוץ Restart, לא מספיק צד אחד;
(2) כן להוסיף משוב חזותי "Waiting for opponent..." כשרק צד אחד הצביע.

- **פרוטוקול**: `ClientCommandType` קיבל ערך חדש `RESTART`. `ClientCommand.isValid()`
  שונה - `RESTART` **פטור** מ-`row`/`col` (אין לו מיקום על הלוח בכלל),
  בניגוד ל-`CLICK`/`JUMP` שחייבים את שניהם. `GameClient.sendRestart()`
  חדש (שולח `{"type":"RESTART","row":0,"col":0}` - ה-0/0 דמה, לא בשימוש
  בצד השרת בכלל).
- **`GameSession` - המשחק צריך "להתאפס"**: לפני זה `board`/`engine`/
  `networkActions` היו `final` (נבנים פעם אחת בבנאי) - כדי לתמוך ב-restart
  הם הפכו לשדות רגילים (mutable), עם `private void resetGame()` חדשה
  שבונה אותם מחדש מ-`boardText` השמור. ה-`EventBus` עצמו נשאר **קבוע
  לאורך חיי ה-session** (לא נבנה מחדש ב-`resetGame()`) - כדי שההרשמה
  ל-`GameLifecycleEvent` (עדכון ELO, Part B) תשרוד גם אחרי restart, לא
  רק אחרי המשחק הראשון.
- **הצבעת Restart**: `Set<ClientRole> restartVotes` (`EnumSet`) - חדש
  ב-`GameSession`. `applyRestartVote(role)`: מתעלם לגמרי אם המשחק עוד
  לא נגמר (`!engine.isGameOver()`) - כדי שלא "יברחו" ממצב הפסד באמצע
  משחק - או אם זה צופה (`SPECTATOR`, אין לו מה "להצביע" בכלל). אחרת
  מוסיף לסט, ואם **גם** WHITE **וגם** BLACK נמצאים בו - קורא ל-`resetGame()`
  (שגם מנקה את הסט בעצמה, כדי שהמשחק הבא יתחיל "נקי").
- **משוב "Waiting for opponent..."**: נוסף שדה בוליאני חדש
  `restartRequestedByViewer` שעובר **דרך כל צינור ה-snapshot** (6 מחלקות:
  `SnapshotMessage`→`IncomingSnapshot`→`ClientSnapshotReconstructor.Reconstructed`
  →`SnapshotFactory`→`GameSnapshot`→`GameSceneView`) - כל שלב מוסיף אותו
  כפרמטר/שדה אחרון, כדי לשמור על תאימות אחורה (הבנאים הישנים של
  `SnapshotMessage` נשארו כ-overload עם `false`, בדיוק כמו ב-Part B, כדי
  ש-`ClientSnapshotReconstructorTest`/`MessageDtoTest` הקיימים ימשיכו
  לעבוד). ב-`GameSceneView.drawGameOverOverlay` - אם `true`: כותרת-משנה
  "Waiting for opponent..." וטקסט כפתור "Waiting..."; אם `false` (עדיין
  לא הצביע/גמר טרי): "Game Over" ו-"Restart".
- **`NetworkClickHandler` - חיבור הקליק בפועל**: קיבל פרמטר שלישי חדש
  לבנאי, `GameSceneView sceneView` (חייב להיבנות **לפני** ה-handler
  ב-`NetworkGameWindowMain` - סדר קונסטרוקציה שהתהפך בכוונה). `handle(...)`
  - אם `gameOver==true`, מפנה ל-`handleRestartClick` חדשה: בודקת האם
  הקליק (אחרי המרה לקואורדינטת-לוח, כמו הקליקים הרגילים) נופל בתוך
  `sceneView.restartButtonBounds()` (`Rectangle.contains`) - אם כן,
  `client.sendRestart()`. אם לא ב-gameOver, אותה התנהגות כמו קודם
  (CLICK/JUMP רגילים).
- **טסטים חדשים**: `ClientCommandTest` (RESTART תקין בלי/עם row+col
  דמה), `GameSessionTest` (4 טסטים חדשים - restart לפני game-over
  מתעלם, צד אחד לא מספיק, שני הצדדים מאפסים בפועל, קול של צופה לא
  נספר - כולם על לוח מזערי חדש `ONE_MOVE_FROM_CAPTURE_BOARD` עם מלך
  שחור+צריח לבן בלבד, כדי שלכידת מלך תהיה מהלך אחד ולא משחק שלם),
  `RecordingGameClient` חדש (מחלקת-בדיקה `extends GameClient`, דורסת
  את שלוש שיטות השליחה כדי רק לרשום `boolean` - כמו `FakeWebSocket`
  הקיים), `NetworkClickHandlerTest` עודכן (הבנאי בן-3-הפרמטרים + שני
  טסטים חדשים לקליק שפוגע/מפספס את הכפתור, עם קואורדינטות שמחושבות
  **מתוך** `restartButtonBounds()` בפועל ולא hard-coded - כדי לא להיתלות
  בקבועי `BUTTON_WIDTH`/`BUTTON_HEIGHT` הפנימיים).
- **אימות שבוצע כאן**: איזון סוגריים + grep להפניות ישנות על **כל**
  הקבצים שנגעו בפיצ'ר הזה - כולם תקינים. **טרם `mvn test` אמיתי** (אין
  לי javac/mvn, ר' מגבלה קבועה למטה) וטרם הרצה ידנית מקצה לקצה.

### README נכתב מחדש (אחרי סבב Restart)

המנחה נתן רשימת דרישות ("which features currently work in your repo"),
ואחד הסעיפים היה "README/run instructions are updated". ה-README שהיה
בפועל היה עדיין ה-boilerplate הגנרי של VS Code Java ("Welcome to the VS
Code Java world...") - בלי שום קשר לפרויקט. נכתב מחדש לגמרי.

- **החלטות שרות קיבלה במפורש**: (1) **בלי Docker** - נשקל והוחלט נגד:
  השרת אמנם headless ומתאים, אבל הלקוח הוא חלון Swing ו-Docker ל-GUI
  דורש X11 forwarding ומסבך יותר משעוזר; גם לא נדרש ברשימת המנחה.
  (2) **אנגלית + סעיף עברי קצר** בסוף.
- **חשוב - רות דחתה במפורש גרסה עם טבלת "מה עשיתי / מה לא"**: הגרסה
  הראשונה כללה טבלת Feature Status (✅/🟡/⬜ לפי רשימת המנחה) + Known
  Limitations + Roadmap. רות: "לא צריך לכתוב ברידמי איזה דברים עשיתי
  ואיזה לא! אני רוצה שיהיה מקצועי כמו המקובל". הוסרו. **לא להחזיר את
  זה** - README מתאר מה המערכת *עושה*, לא דיווח-עצמי על סטטוס.
- **המבנה הסופי**: כותרת ממורכזת + badges → Overview → Features →
  Table of Contents → Quick Start (prerequisites/build/run) → Controls →
  Architecture (דיאגרמת Mermaid + design decisions) → Protocol (טבלאות
  הודעות לשני הכיוונים) → Project Structure → Testing → Tech Stack →
  הוראות הרצה בעברית.
- **טעות שנתפסה תוך כדי**: כתבתי בהתחלה פקודות `mvn exec:java`, אבל אין
  `exec-maven-plugin` ב-`pom.xml` - הן היו נכשלות. הוסרו; ההוראות
  מתארות הרצה מ-IntelliJ בלבד, שעובדת בוודאות.

### שלב 5, חלק 1 - ניתוק/auto-resign עם חלון חסד 20 שניות (הסבב הזה)

רות אישרה שני החלטות פתוחות לפני שהתחלנו: (1) ניתוק **לא** מפסיד מיד -
יש **חלון חסד של 20 שניות** לחיבור מחדש (אותו username בדיוק); (2) כן
להוסיף אינדיקציה חזותית ("Opponent disconnected - Xs to reconnect"),
בדומה ל"Waiting for opponent..." של Restart.

- **`GameEngine.forceGameOver(PieceColor winner)`** (חדשה, ציבורית) -
  הוצאה מ-`checkForKingCapture` (שתי השורות: `game.markGameOver` +
  פרסום `GameLifecycleEvent(ENDED,...)`) לפונקציה משותפת אחת, כדי
  שגם לכידת מלך וגם ניתוק/timeout יעברו באותה נקודה יחידה - בלי לשכפל
  את "מה קורה כשמשחק נגמר" בשני מקומות.
- **`GameSession` - התור השני (בנוסף ל-pendingCommands)**: `handleDisconnect(WebSocket)`
  חדשה, נקראת מ-`GameServer.onClose` **במקום** `removeConnection` הישנה
  (שנשארה כמות שהיא - עדיין נקראת ישירות מ-`GameSessionTest` ומ-`processDisconnections`
  עצמה). קריטי: `handleDisconnect` **לא נוגעת ב-`connections`/`engine` בעצמה בכלל** -
  היא רק מתייקת את החיבור ל-`pendingDisconnections` (עוד `ConcurrentLinkedQueue`,
  בדיוק כמו `pendingCommands`), כי היא רצה מ-thread הרשת (`GameServer.onClose`)
  ואסור לה לגעת ב-engine משם. `tick()` (thread הטיק היחיד) הוא זה שבאמת
  מעבד ניתוקים, דרך `processDisconnections()` חדשה שרצה **לפני** לולאת
  הפקודות: לכל חיבור שהצטבר - אם צופה או שהמשחק כבר נגמר, מסירה בלבד
  (כמו ה-`onClose` המקורי); אחרת (WHITE/BLACK באמצע משחק פעיל) שומרת
  `PendingDisconnect(username, deadlineMillis)` חדש ב-`pendingDisconnects`
  (`EnumMap<ClientRole,...>`) - הדדליין מחושב לפי **`engine.now()`** (שעון
  המשחק, לא `System.currentTimeMillis()`) בדיוק כמו `PieceTimers`/`CaptureEffectTracker`
  הקיימים - כך `GameSessionTest` יכול "לקפוץ" 20 שניות קדימה ב-`tick(25_000)`
  אחד בלי `Thread.sleep` אמיתי, ואין תלות בזמן-קיר.
- **`resolveExpiredDisconnects()`** (חדשה, נקראת בסוף `tick()`) - אם דדליין
  כלשהו עבר: הצד השני מוכרז מנצח דרך `engine.forceGameOver(winner)`
  (אותה נקודה שגם לכידת מלך משתמשת בה - עדכון ה-ELO הקיים דרך
  `GameLifecycleEvent(ENDED)` קורה אוטומטית, בלי קוד חדש), ואז `pendingDisconnects.clear()`.
- **`assignRole` - reconnect**: לפני שקובעים תפקיד "רגיל", `tryReconnect(connection, username)`
  חדשה בודקת אם יש חלון-חסד פתוח עם **אותו username בדיוק** - אם כן,
  מבטלת אותו ומחזירה לחיבור החדש את אותו תפקיד. `isRoleOccupied(role)`
  חדשה - "תפוס" עכשיו אומר "יש חיבור חי, **או** יש חלון-חסד פתוח על
  התפקיד הזה" - כדי שאף אחד אחר לא "יגנוב" את הצבע שהתפנה תוך כדי
  שהשחקן המקורי עדיין עשוי לחזור. **מגבלה מודעת**: reconnect מזוהה
  אך ורק לפי username - שחקן שהתחבר בלי login (אנונימי) לא יכול לחזור
  אחרי ניתוק, גם בתוך חלון החסד (התנהגותית שקול להפסד מיידי עבורו/ה).
- **`usernameFor(role)`** תוקן - נופל חזרה ל-`pendingDisconnects` אם אין
  חיבור חי (כדי ש-ELO עדיין ידע למי להוריד דירוג אחרי שהחיבור כבר הוסר).
- **בעיית concurrency שנתפסה תוך כדי (ולא הייתה קודם)**: `pendingDisconnects`
  הוא `EnumMap` רגיל (לא thread-safe כמו `connections`/`pendingCommands`
  שהם `ConcurrentHashMap`/`ConcurrentLinkedQueue`) - ונקרא/נכתב גם
  מ-thread הטיק (`tick`/`processDisconnections`/`resolveExpiredDisconnects`)
  **וגם** מ-thread הרשת (`assignRole`, דרך `tryReconnect`/`isRoleOccupied`).
  התיקון: `tick()` ו-`snapshotFor()` הפכו ל-`synchronized` (כמו ש-`assignRole`
  כבר היה) - כל הגישה ל-`pendingDisconnects` עוברת עכשיו דרך אותו מנעול
  (`this`) בין שני ה-threads.
- **שידור למרחוק (אותו דפוס בדיוק כמו `restartRequestedByViewer`)**:
  `disconnectSecondsRemaining()` חדשה ב-`GameSession` (מחשבת שניות
  שנותרו לפי `engine.now()`, `Integer` ולא `int` כדי ש-`null` יבדיל "אין
  ניתוק פעיל" מ-"0 שניות נשארו") מתווספת ל-`snapshotFor`, ועוברת דרך
  אותן 5 מחלקות בדיוק: `SnapshotMessage` (עם overload ישן, בלי השדה,
  שקול ל-`null` - כדי ש-`MessageDtoTest`/`ClientSnapshotReconstructorTest`
  הקיימים ימשיכו לעבוד בלי שינוי) → `IncomingSnapshot` → `ClientSnapshotReconstructor.Reconstructed`
  → `SnapshotFactory.createSnapshot` → `GameSnapshot`. **שונה במכוון**
  מ-motions/jumps: השרת שולח מספר-שניות מוכן במקום timestamp גולמי
  שהלקוח מחשב ממנו (כמו שהוא עושה לאנימציות) - כי זו רק תצוגת טקסט
  סטטית, לא אנימציה חלקה, אז אין צורך בדיוק כזה.
- **`GameSceneView.drawDisconnectBanner(...)`** (חדשה) - פס אזהרה צר
  לרוחב הלוח, צמוד לקצה העליון: "Opponent disconnected - Xs to reconnect".
  נקרא מ-`render()` כש-`snapshot.disconnectSecondsRemaining() != null`,
  **בלי תלות** ב-`if (snapshot.gameOver())` הקיים (drawGameOverOverlay) -
  זה בכוונה: ניתוק קורה **באמצע** משחק פעיל (`gameOver=false`), אז זה
  לא יכול להיות אותו overlay מלא-מסך כמו סוף משחק.
- **טסטים חדשים** (`GameSessionTest`): ניתוק באמצע משחק פותח חלון חסד
  בלי הפסד מיידי; פקיעת הזמן (`tick` עם elapsed גדול) מכריזה על היריב
  כמנצח; reconnect עם username תואם מבטל את החלון ומחזיר תפקיד; חלון
  שמור לא נגנב ע"י חיבור אחר; ניתוק צופה מיידי כרגיל; ניתוק אחרי שהמשחק
  כבר נגמר לא פותח חלון חדש. גם `MessageDtoTest`/`ClientSnapshotReconstructorTest`
  קיבלו טסט לשדה החדש (כולל שהוא נעדר מה-JSON כש-`null`, לא נכתב כ-"null").
- **אימות שבוצע כאן**: איזון סוגריים + grep להפניות ישנות שנשברו על **כל**
  הקבצים שנגעו בפיצ'ר הזה - כולם תקינים. **טרם `mvn test` אמיתי וטרם
  הרצה ידנית מקצה לקצה** (אין לי javac/mvn, ר' מגבלה קבועה למטה) - ר'
  "מה שנשאר לאמת" למטה לפירוט המדויק.

### שלב 5, חלק 2 - Matchmaking (כפתור "Skip") (הסבב הזה)

רות דיווחה בעיה בפועל אחרי שהתחילה לבדוק ידנית את שלב 5 חלק 1: כשיש
כבר 2 שחקנים ב-room ברירת המחדל ("default") ומתחבר/ת שלישי/ת בלי
להקליד room אחר בעצמה/ו, אין דרך לפתוח משחק חדש - היא/הוא פשוט נכנס/ת
כ-SPECTATOR. זו בדיוק הבעיה ששלב 5 חלק 2 אמור לפתור. הסיכום שאושר:
מסך הבית מקבל **כפתור "Skip" נוסף**, בנוסף (לא במקום) לשדה ה-room +
כפתור Connect הקיימים - Connect ממשיך להתנהג בדיוק כמו היום (חדר לפי
שם), Skip מתעלם משם ה-room לגמרי ומבקש מהשרת "תמצא/י לי משחק".

- **`GameSession.isWaitingForOpponent()`** (חדשה, public, `synchronized`
  - קוראת מ-`pendingDisconnects` בדיוק כמו `tick()`/`snapshotFor()`, אותו
  מנעול) - `true` רק אם יש **בדיוק** צד אחד (WHITE או BLACK) מחובר בפועל,
  הצד השני **גם לא מחובר וגם לא שמור לו חלון-חסד** (`pendingDisconnects`,
  שלב 5 חלק 1), והמשחק לא נגמר. הבדיקה נגד `pendingDisconnects` קריטית:
  בלעדיה, מישהו/י שממתין/ה לחיבור-מחדש של היריב/ה המקורי/ת (אחרי ניתוק
  זמני) היה/הייתה "נחטף/ת" בטעות ע"י שחקן/ית אקראי/ת מה-matchmaking.
- **`kfchess.server.server.MatchmakingResolver`** (מחלקה חדשה, מקבילה
  ל-`GameIdResolver`) - `isMatchmakingRequest(resourceDescriptor)`: בודקת
  אם הנתיב (אחרי חיתוך query string, אותה שיטה בדיוק כמו `GameIdResolver`)
  שווה לטוקן שמור `"_play"`. מחלקה טהורה נפרדת - נבדקת ביחידה בלי
  handshake מזויף, כמו `GameIdResolver`.
- **`GameServer`**:
  - שדה חדש `matchmakingLock` (אובייקט נעילה **נפרד** מהנעילות הפנימיות
    של `GameSession` עצמה - כאן הבעיה היא ברמת "איזה session בכלל
    נבחר", לא מה קורה בתוכו).
  - **`resolveMatchmakingGameId()`** (חדשה, private, `synchronized(matchmakingLock)`)
    - עוברת על כל ה-sessions, מחפשת אחד עם `isWaitingForOpponent()==true`
    ומחזירה את ה-id שלו; אם לא מצאה - יוצרת id חדש וייחודי (`"match-"+UUID`,
    לא ניתן להקליד בטעות בשדה room ידני) ופותחת session חדש. **חייבת**
    להיות מסונכרנת: בלי זה, שני אנשים שלוחצים "Skip" כמעט בו-זמנית
    עלולים שניהם *לא* לראות אחד את השני (כי אף session עדיין לא נוצר)
    ולפתוח כל אחד/ת session נפרד - ולעולם לא להיפגש.
  - **`onOpen`** משתנה קלות: קודם בודקת `MatchmakingResolver.isMatchmakingRequest(...)`
    - אם כן, `gameId=resolveMatchmakingGameId()`; אחרת בדיוק ההתנהגות
    הקיימת (`GameIdResolver.resolve(...)`, חדר-בשם-ספציפי, לא השתנה).
  - **מגבלה ידועה, לא טופלה**: הבחירה בין כמה sessions ממתינים היא "הראשון
    שנמצא בסריקה" - לא תור FIFO אמיתי לפי כמה זמן מישהו/י ממתין/ה.
- **`HomeScreenMain`**:
  - כפתור "Skip" חדש ליד "Connect" הקיים.
  - **`buildMatchmakingUri(username)`** (חדשה, public, טהורה - נבדקת בלי
    UI) - בונה `ws://.../_play` + `?username=...` אם יש, אותה שיטת קידוד
    בדיוק כמו `buildUri`. הטוקן `"_play"` מוגדר כאן **בנפרד** מ-`MatchmakingResolver`
    (לא משותף ע"י מחלקת קבועים אחת) - אותו עיקרון בדיוק כמו ש-`DEFAULT_ROOM`
    כאן ו-`DEFAULT_GAME_ID` ב-`GameIdResolver` כבר מוגדרים בנפרד היום.
  - **`connect(...)`** שונתה: במקום לקבל `room` ולבנות URI בפנים, מקבלת
    `uriText` **מוכן** - כך ש-Connect (`buildUri`) ו-Skip (`buildMatchmakingUri`)
    קוראים לאותה מתודה בדיוק, בלי כפילות קוד. גם משביתה עכשיו את **שני**
    הכפתורים (לא רק זה שנלחץ) בזמן חיבור - כדי שלא אפשר ללחוץ בטעות על
    השני ולפתוח שני חיבורים.
- **טסטים חדשים**: `GameSessionTest` (5 טסטים ל-`isWaitingForOpponent` -
  צד אחד מחובר, אף אחד לא מחובר, שני הצדדים מחוברים, צד שני שמור בחלון-חסד,
  משחק כבר נגמר), `MatchmakingResolverTest` חדש (6 טסטים, מקביל ל-`GameIdResolverTest`),
  `HomeScreenMainTest` (3 טסטים ל-`buildMatchmakingUri`).
- **אימות שבוצע כאן**: איזון סוגריים + grep להפניות ישנות שנשברו על כל
  הקבצים שנגעו בפיצ'ר הזה - כולם תקינים. **טרם `mvn test` וטרם הרצה
  ידנית מקצה לקצה** (אין לי javac/mvn) - ר' "מה שנשאר לאמת" למטה.

### שלב 6, חלק 1 - חדרים: Create/Join/Cancel (הסבב הזה)

לפני זה, שדה room חופשי + כפתור "Connect" ב-`HomeScreenMain` נתנו דרך
לא-רשמית לעשות את אותו הדבר (זו הייתה תוספת-תשתית מסבב קודם, לא חלק
מהדרישה המקורית בכלל). **הוחלפו** לגמרי (לא נשארו לצד הדיאלוג החדש) -
רות אישרה במפורש.

- **`RoomIdGenerator`** (מחלקה חדשה, טהורה) - `generate()`: קוד קצר
  (6 תווים, אותיות גדולות+ספרות) - בכוונה **לא** UUID כמו ב-matchmaking:
  הקוד הזה צריך "להיכתב על המסך" ושמישהי אחרת תקליד אותו כדי להצטרף
  (Join) - UUID ארוך מדי לזה בפועל.
- **`CreateRoomResolver`** (מחלקה חדשה, מקבילה ל-`MatchmakingResolver`) -
  `isCreateRoomRequest(path)`: בודקת נתיב שמור `"_create"` (שונה מ-`"_play"`
  של matchmaking - שני נתיבים שמורים עצמאיים).
- **`GameServer`**:
  - **`createNewRoomGameId()`** (חדשה) - מייצרת קוד עם `RoomIdGenerator`,
    בודקת מול `sessions` שהוא לא תפוס (לולאת retry אם כן - נדיר מאוד),
    פותחת session חדש. עטופה באותה נעילה ששימשה קודם רק את matchmaking -
    שונה שם מ-`matchmakingLock` ל-**`sessionAllocationLock`** (שם כללי
    יותר, כי עכשיו שתי פעולות שונות חולקות אותה: "תמצא/י או תמציא/י
    gameId פנוי ושמרי אותו לפני שמישהו אחר עושה בדיוק אותו דבר").
  - **`onOpen`** - ענף שלישי (נבדק ראשון): אם `CreateRoomResolver.isCreateRoomRequest`
    → `createNewRoomGameId()`; אחרת matchmaking כמו קודם; אחרת חדר-בשם
    (Join, `GameIdResolver` - **בלי שינוי בכלל**, "Join" זה בדיוק אותו
    flow שכבר היה).
- **הצגת ה-gameId "בראש המסך" (הדרישה המדויקת)**:
  - **`Img.setTitle(String)`** (חדשה, סטטית) - כותרת חלון המשחק. תומכת
    בקריאה *לפני* שה-frame נוצר בפועל (הוא נוצר באיחור, בתוך `invokeLater`
    משלה בפעם הראשונה - ר' `show()`) ע"י שמירת `pendingTitle` שמוחלת
    ברגע היצירה.
  - **`IncomingMessageSummary.isRoleAssigned(json)`** (חדשה, טהורה) -
    מזהה הודעת ROLE_ASSIGNED (מקבילה ל-`isSnapshot`).
  - **`GameClient.assignedGameId()`** (חדשה) - שדה `volatile` חדש שנחתך
    מתוך ה-JSON ב-`onMessage` כש-`isRoleAssigned` - זה ה-gameId **בפועל**
    שהשרת הקצה (קריטי ל-Create: הלקוח לא ידע אותו מראש בכלל, השרת המציא אותו).
  - **`NetworkGameWindowMain.launch(GameClient, String gameId)`** - קיבלה
    פרמטר `gameId` חדש (שינוי חתימה, לא overload - הקריאה היחידה אליה
    היא מ-`HomeScreenMain`, אין טסטים ישירים ל-`launch`), קוראת ל-
    `Img.setTitle("KFChess - Room: " + gameId)` בתחילת המתודה.
  - **`HomeScreenMain.waitForAssignedGameId(GameClient)`** (חדשה, private) -
    אחרי `connectBlocking()` (עדיין על thread הרקע, לא ה-EDT), ממתינה
    (poll כל 20ms, עד 2 שניות timeout) ל-`client.assignedGameId()` -
    כי ROLE_ASSIGNED מגיעה כהודעת רשת נפרדת, לא בהכרח כבר הגיעה ברגע
    ש-`connectBlocking()` חוזר.
- **`HomeScreenMain` - שינוי ה-UI**:
  - כפתורי מסך הבית: **"Play"** (היה "Skip" - שם הכפתור עצמו שונה כדי
    להתקרב למפרט; הלוגיקה מאחוריו **לא** שונתה - עדיין הפער הידוע משלב 5)
    + **"Room..."** חדש (במקום שדה room+Connect).
  - **`showRoomDialog(...)`** (חדשה) - `JDialog` מודלי עם תיבת טקסט +
    Create/Join/Cancel, בדיוק לפי המפרט. Join עם תיבה ריקה **לא** מתחברת
    בכלל (מציגה הודעה מקומית בדיאלוג) - כדי שלא "יתגלגלו" בטעות לחדר
    `default` המשותף (ההתנהגות הזו של `buildUri` עצמה נשארה ללא שינוי,
    לתאימות הטסטים הקיימים - רק ה-UI לא נותן להגיע לזה).
  - **`buildCreateRoomUri(username)`** (חדשה, ציבורית, טהורה) - מקבילה
    ל-`buildMatchmakingUri`, נתיב `_create`.
  - **`connect(...)`/`showFailure(...)`** - שונו ל-varargs (`JButton...
    buttonsToToggle`) במקום פרמטרים קשיחים - כל כפתורי מסך הבית מושבתים
    יחד בזמן חיבור, לא רק זה שנלחץ, כדי שלא אפשר לפתוח בטעות שני חיבורים.
- **טסטים חדשים**: `RoomIdGeneratorTest` (פורמט + "שתי קריאות שונות"),
  `CreateRoomResolverTest` (6 טסטים, מקביל ל-`MatchmakingResolverTest`),
  `IncomingMessageSummaryTest` (2 טסטים ל-`isRoleAssigned`), `HomeScreenMainTest`
  (3 טסטים ל-`buildCreateRoomUri`). `GameClient.assignedGameId`/`waitForAssignedGameId`
  לא נבדקו ביחידה ישירות (דורשים שרת חי - כמו כל שאר `GameClient`/`connect`, לא שונה).
- **אימות שבוצע כאן**: איזון סוגריים + grep להפניות ישנות שנשברו על כל
  הקבצים - תקינים. **טרם `mvn test` וטרם הרצה ידנית** - ר' "מה שנשאר לאמת" למטה.
- **עדיין לא בוצע בסבב הזה** (שלב 6 חלק 2, סבב נפרד): לוגים בצד שרת
  וגם בצד לקוח, על כל הפעילות - רות בחרה "קובץ טקסט" כפורמט השמירה.

### תיקון - Play לא אמור להצטרף לחדרים פרטיים (הסבב הזה)

רות דיווחה: הרצה של שני לקוחות, אחד/ת עשה/תה Create (חדר פרטי) והשני/ה
לחצ/ה Play (matchmaking) - שניהם נכנסו לאותו חדר. זה באג: Play אמור
להתאים רק למי שגם הוא/היא לחצ/ה Play, לא "לגנוב" חדר פרטי שממתין
לחברה ספציפית עם קוד.

- **הסיבה**: `GameServer.resolveMatchmakingGameId()` סרקה את **כל**
  ה-sessions וחיפשה כל אחד שממתין ליריב (`isWaitingForOpponent()`) -
  בלי לבדוק בכלל איך ה-session נוצר. חדר פרטי עם רק WHITE מחובר/ת
  "נראה" בדיוק כמו session שממתין ל-matchmaking.
- **התיקון**: שדה חדש ב-`GameServer` - `Set<String> matchmakingSessionIds`
  (`ConcurrentHashMap.newKeySet()` - נגיש גם מ-thread הרשת). כל gameId
  שנוצר *דרך* `resolveMatchmakingGameId()` עצמה (כשלא נמצא match קיים)
  נוסף לסט. הסריקה עכשיו דורשת גם `isWaitingForOpponent()` וגם
  `matchmakingSessionIds.contains(entry.getKey())` - חדרים מ-Create
  (`RoomIdGenerator`) וחדרים מ-Join-לפי-שם (`GameIdResolver`) לעולם לא
  נכנסים לסט, ולכן Play לעולם לא יתפוס אותם.
- **אימות שבוצע כאן**: איזון סוגריים - תקין. **אין ל-`GameServer` טסטים
  ייחודיים** (בדיוק כמו `GameClient` - דורש שרת חי אמיתי) - טעון בדיקה
  ידנית בלבד, ר' "מה שנשאר לאמת" למטה.

### פיצ'ר חדש (בקשת רות) - לא ניתן להתחיל לשחק בזמן המתנה ליריב

בקשת רות: כל עוד רק צד אחד (WHITE או BLACK) מחובר/ת ואין עדיין יריב/ה -
אסור לצד המחובר להזיז/לבחור כלים בכלל, עד שהיריב/ה מצטרף/ת בפועל.

- **`GameSession.applyCommand`** - הוספה בדיקה חדשה: אם `isWaitingForOpponent()`
  מחזירה true - פקודות CLICK/JUMP מתעלמות בשקט (return מוקדם, בלי הודעת
  שגיאה ללקוח - פשוט שום דבר לא קורה). הבדיקה ממוקמת *אחרי* הטיפול
  ב-RESTART (RESTART ממילא רלוונטי רק כש-`engine.isGameOver()`, ואז
  `isWaitingForOpponent()` תמיד false ממילא - אין התנגשות אמיתית, רק
  סדר בדיקות הגיוני).
  שימוש חוזר ב-`isWaitingForOpponent()` הקיימת (נבנתה לשלב 5 חלק 2,
  matchmaking) - בלי לשכפל לוגיקה: "יש בדיוק צד אחד מחובר והשני לא
  תפוס גם לא ע"י חלון-חסד" נשאר קריטריון אחד ויחיד לשני השימושים
  (matchmaking + חסימת מהלכים).
  קריאה בטוחה מבחינת thread: `isWaitingForOpponent()` היא `synchronized(this)`,
  ו-`applyCommand` תמיד רץ מתוך `tick()` שכבר מחזיק את אותו מנעול - נעילה
  חוזרת (reentrant), לא deadlock.
  שעון המשחק (`engine.handleWait`) ממשיך לרוץ כרגיל בזמן ההמתנה - רק
  הקליקים עצמם נחסמים, לא הוספה שום עצירה של הטיימר.
  **עדכון**: בהתחלה לא נוספה אינדיקציה ויזואלית - רות ביקשה בנפרד
  שתהיה כזו, ר' הסעיף הבא "אינדיקציה ויזואלית - Waiting for an opponent".
- **טסטים חדשים ב-`GameSessionTest`**: `tick_clickWhileWaitingForOpponent_isIgnored`
  (WHITE לבד/ה, קליק לא עובד - אין `selected` ב-snapshot),
  `tick_clickAfterOpponentJoins_isProcessedNormally` (אותו קליק אחרי
  ש-BLACK מצטרף/ת - עכשיו כן עובד).
- **אימות שבוצע כאן**: איזון סוגריים - תקין. **טרם `mvn test`** - ר' "מה
  שנשאר לאמת" למטה.

**עדכון - רות הריצה `mvn test` בפועל ומצאה רגרסיה**: שני טסטים ישנים
נכשלו - `tick_clickThenLegalTarget_movesPieceIntoTransit` ו-
`tick_clickThenLegalTarget_snapshotIncludesMatchingMotion`. הסיבה: שני
הטסטים האלה (שנכתבו הרבה לפני הפיצ'ר הזה) רשמו רק חיבור WHITE יחיד
בכוונה (כדי לבדוק קליק/תזוזה בפשטות, בלי צורך אמיתי בשני צדדים ללוגיקה
שהם בודקים) - אבל עכשיו `isWaitingForOpponent()` חוסמת בדיוק את זה,
כי אין BLACK מחובר. **תוקן**: הוספתי `session.assignRole(new FakeWebSocket())`
(BLACK) לשני הטסטים, בדיוק כמו שכל שאר הטסטים ב-`GameSessionTest`
שבודקים קליקים/מהלכים כבר עושים. זו לא "עקיפה" של הפיצ'ר - זה בדיוק
התיקון הנכון: הטסטים בודקים לוגיקת מהלכים, לא לוגיקת המתנה, אז הם
צריכים לדמות משחק "שהתחיל" (שני צדדים מחוברים), בדיוק כמו שקורה בפועל.

### אינדיקציה ויזואלית - "Waiting for an opponent..." (בקשת המשך, הסבב הזה)

רות ביקשה שגם יהיה רואים ויזואלית שממתינים ליריב (בנוסף לחסימת הקליקים
למעלה) - נבנה בדיוק לפי אותו דפוס שכבר קיים לבאנר הניתוק
(`drawDisconnectBanner`), רק צבע/טקסט שונים.

השדה `isWaitingForOpponent()` הועבר עד ללקוח דרך **כל שכבות ה-snapshot**
(אותו "צינור" בדיוק ש-`disconnectSecondsRemaining` כבר עובר בו, שלב 5):

- **`SnapshotMessage`** - שדה `boolean waitingForOpponent` חדש. נוסף
  בנאי מלא חדש (עם הפרמטר הזה בסוף), וה"מלא" הקודם (עם
  `disconnectSecondsRemaining`) הפך לחתימת-תאימות שמעבירה `false` -
  בדיוק הדפוס שכבר קיים כאן לכל שדה חדש שנוסף (ר' ההיסטוריה של
  הקובץ הזה - restartRequestedByViewer ו-disconnectSecondsRemaining
  נוספו באותה שיטה בדיוק).
- **`GameSession.snapshotFor`** - מעביר `isWaitingForOpponent()` לבנאי
  החדש. אין בעיית thread: גם `snapshotFor` וגם `isWaitingForOpponent`
  הן `synchronized(this)` - נעילה חוזרת, לא deadlock (אותו נימוק כמו
  ב-`applyCommand` למעלה).
- **`IncomingSnapshot`** (צד לקוח) - שדה מראה `waitingForOpponent` + getter.
- **`ClientSnapshotReconstructor.Reconstructed`** - שדה נוסף בסוף ה-record,
  מועבר הלאה בלי שום עיבוד (בדיוק כמו `disconnectSecondsRemaining`  -
  אין כאן "שחזור זהות" לעשות, רק מעביר את הערך כמו שהוא).
- **`GameSnapshot`** (record) + **`SnapshotFactory.createSnapshot`** -
  פרמטר/שדה נוסף בסוף, מועבר עד לצייר.
- **`NetworkGameWindowMain`** - עודכנו שני מקומות: הקריאה ל-`createSnapshot`
  (מעבירה `state.waitingForOpponent()`) וגם `emptyReconstructedBeforeFirstSnapshot`
  (ה"מצב ריק" לפני snapshot ראשון - `false`, כי עוד אין מספיק מידע לדעת).
- **`GameSceneView`**:
  - קבוע צבע חדש `WAITING_BANNER_BACKGROUND` (כחול רגוע - שונה בכוונה
    מהכתום-אדמדם `DISCONNECT_BANNER_BACKGROUND` של ניתוק: זו לא "אזהרה",
    רק מידע נייטרלי).
  - **שינוי שם** (לא לוגי, רק ניקיון): `DISCONNECT_BANNER_HEIGHT`/`DISCONNECT_BANNER_FONT_SIZE`
    → `TOP_BANNER_HEIGHT`/`TOP_BANNER_FONT_SIZE` - אותה גיאומטריה בדיוק
    משמשת עכשיו את שני סוגי הבאנר (ניתוק + המתנה ליריב), אין טעם
    בשני קבועים זהים בשם שונה. אין ל-`GameSceneView` טסטים ייחודיים
    (בדק grep) - שינוי השם לא שובר כלום.
  - `render()` - `if` עצמאי חדש: אם `snapshot.waitingForOpponent()` -
    `drawWaitingForOpponentBanner(...)`. **לא תלוי** ב-`if` של הניתוק -
    בפועל הם אף פעם לא קורים בו-זמנית (מוכח: `isWaitingForOpponent()`
    דורשת שהצד השני יהיה "פנוי" *וגם* לא שמור בחלון-חסד, אז אם יש
    חלון-חסד פתוח בכלל, `isWaitingForOpponent()` תמיד false) - אבל
    בכוונה נשאר בלי תלות מפורשת, אותו עיקרון כמו ה-`if` של gameOver/ניתוק.
  - **`drawWaitingForOpponentBanner`** (חדשה) - מציירת פס עליון לרוחב
    הלוח: "Waiting for an opponent to join..." - בדיוק אותה גיאומטריה
    כמו `drawDisconnectBanner`, רק צבע שונה.
- **טסטים חדשים**: `GameSessionTest` - `snapshotFor_onlyWhiteConnected_reportsWaitingForOpponentTrue`,
  `snapshotFor_bothSidesConnected_reportsWaitingForOpponentFalse`. `MessageDtoTest` -
  `snapshotMessage_withWaitingForOpponentTrue_serializesField`,
  `snapshotMessage_oldConstructorWithoutWaitingForOpponent_defaultsToFalse`.
  `ClientSnapshotReconstructorTest` - `reconstruct_waitingForOpponent_passedThroughUnchanged`.
- **אימות שבוצע כאן**: איזון סוגריים על כל הקבצים שנגעתי בהם - תקין,
  grep ל-`DISCONNECT_BANNER_HEIGHT`/`DISCONNECT_BANNER_FONT_SIZE` הישנים
  (ודא ששום מקום לא נשאר תלוי בשם הישן) - נקי. **טרם `mvn test` וטרם
  הרצה ידנית** - ר' "מה שנשאר לאמת" למטה.

### לוגים - שלב 6, חלק 2 (הסבב הזה)

הציטוט המדויק מהמנחה: "Store logs on both server and client side, for
all of the client/server activity". **חשוב להבחין**: זה **לא** אותו
דבר כמו `moveLog` (רישום מהלכי השחמט, שכבר קיים משלבים מוקדמים ומוצג
בפאנל הצדדי) - זה לוג טכני/תפעולי נפרד לגמרי (מי התחבר/התנתק, שגיאות,
איך משחק נוצר) - הובהר בצ'אט עם רות באריכות לפני המימוש.

**החלטות עיצוב שרות בחרה במפורש**: פורמט **קובץ טקסט** (לא DB/JSON),
ו-**קובץ חדש בכל הרצה** (לא קובץ אחד שמצטבר) - כדי שכל תהליך (כל חלון
לקוח, וגם השרת) יכתוב לקובץ ייחודי משלו, בלי סיכון "להתערבב" עם תהליכים
אחרים שרצים בו-זמנית.

- **`kfchess.logging.FileLogger`** (מחלקה חדשה, משותפת לשרת וללקוח) -
  `new FileLogger(prefix)` יוצרת תיקייה `logs/` (אם אין) ופותחת קובץ
  `logs/<prefix>_<yyyy-MM-dd_HH-mm-ss>.log` (חותמת זמן בשם הקובץ - קובץ
  ייחודי לכל הרצה, בדיוק כמו שרות ביקשה). `log(String)` - `synchronized`,
  מוסיפה שורה עם חותמת זמן (שעה:דקה:שנייה.מילישנייה) בתחילתה, `autoFlush=true`
  (נכתב לדיסק מיד, לא ממתין לסגירה - חשוב אם התהליך קורס). אם אי-אפשר
  לפתוח את הקובץ בכלל (בעיית הרשאות למשל) - לא מפילה את השרת/לקוח,
  רק מדפיסה אזהרה ל-`System.err` וממשיכה בלי כתיבה לקובץ (`log()` בודקת
  null בשקט). `close()` קיימת לניקיון עתידי אבל לא נקראת כרגע בפועל
  (autoFlush כבר מבטיח שהתוכן על הדיסק בלי צורך ב-close מפורש).
- **`GameServer`** - שדה `FileLogger fileLogger = new FileLogger("server")`
  אחד לכל תהליך שרת (לא לכל session - "פעילות שרת" היא ברמת GameServer).
  נקודות לוג: `onStart` (השרת עלה, איזה פורט), `onOpen` (gameId, תפקיד
  שהוקצה, username אם יש, ו**איך** התחבר/ה - Create/Play/Join, נגזר
  מאותה בדיקה שכבר קובעת את ה-gameId), `onClose` (gameId, קוד, סיבה),
  `onError`. אלה בדיוק אותן נקודות שכבר היה בהן `System.out.println`/
  `System.err.println` - רק נוספה גם כתיבה לקובץ, בלי לגעת בהתנהגות הקיימת.
- **`GameClient`** - שדה `FileLogger fileLogger = new FileLogger("client")`
  אחד לכל **מופע** `GameClient` - כלומר אחד לכל ניסיון חיבור (`HomeScreenMain.connect`
  יוצרת `GameClient` חדש בכל לחיצת Play/Room), מתאים בדיוק ל"קובץ בכל
  הרצה". נקודות לוג: `onOpen` (התחברות הצליחה), `onMessage` (רק הודעות
  שאינן SNAPSHOT - אותו כלל שכבר חל על הדפסה לקונסולה, אחרת 30 שורות
  בשנייה בלי תועלת; משתמש ב-`IncomingMessageSummary.describe` הקיים),
  `onClose`, `onError`, `sendClick`/`sendJump`/`sendRestart` (מה שנשלח,
  לפני השליחה בפועל).
- **טסטים חדשים**: `FileLoggerTest` - שתי בדיקות (`log_writesTimestampedLineToFileUnderLogsDirectory`,
  `log_multipleCalls_appendsEachAsSeparateLine`) שכותבות בפועל ל-`logs/`
  (כמו בייצור) עם קידומת ייחודית לכל טסט כדי לא להתנגש. **`GameServer`/
  `GameClient` עדיין בלי טסטים ייחודיים** (דורשים שרת/רשת חיים - לא
  השתנה בסבב הזה, אותו מצב כמו קודם).
- **הערה על `.gitignore`**: כבר יש שם `*.log` (שורה קיימת, לא הוספתי) -
  אז כל קבצי הלוג (גם מהרצה אמיתית, גם מהטסטים) כבר מוחרגים אוטומטית
  מה-git, אין צורך בשינוי. תיקיית `logs/` עצמה לא תופיע ב-`git status`
  כי היא (אחרי שהיא ריקה מקבצים לא-מוחרגים) - git ממילא לא עוקב אחרי
  תיקיות ריקות.
- **אימות שבוצע כאן**: איזון סוגריים על `FileLogger.java`/`FileLoggerTest.java`/
  `GameServer.java`/`GameClient.java` - תקין. **טרם `mvn test` וטרם הרצה
  ידנית** (לוודא שקבצי הלוג באמת נוצרים ומתמלאים כמצופה) - ר' "מה
  שנשאר לאמת" למטה.

### תיקון "Play" לפי המפרט המדויק - ELO ±100 / timeout של דקה / הודעת "לא נמצא" (הסבב הזה)

הפריט האחרון שנשאר פתוח מכל הפרויקט - הציטוט המדויק (כבר תועד למעלה,
חוזר כאן לנוחות): "Finds the other player with ELO in range of ±100
that also seeks for a game. If doesn't find - waits for 1 min, if
can't find - pops up a message that can't find." רות אישרה לממש עכשיו.

- **`GameSession.waitingPlayerUsername()`** (מתודה חדשה, `synchronized`) -
  ה-username של הצד היחיד שכבר מחובר, כש-`isWaitingForOpponent()`=true
  (אחרת `Optional.empty()` - גם אם אין בכלל מי שממתין, וגם אם מי
  שממתין/ה לא התחבר/ה עם login). נחוצה כדי ש-`GameServer` ידע **של מי**
  לבדוק ELO מול המחפש/ת החדש/ה - שימוש חוזר מלא ב-`isWaitingForOpponent()`
  הקיימת, בלי כפילות לוגיקה.
- **`GameServer.resolveMatchmakingGameId(String searcherUsername)`** -
  שינתה חתימה (קיבלה פרמטר `searcherUsername` חדש - `onOpen` מזיז את
  חילוץ ה-username *לפני* חישוב ה-gameId, כי הפונקציה הזו צריכה אותו
  עכשיו). הסריקה על ה-sessions הממתינים (עדיין רק בתוך `matchmakingSessionIds`,
  התיקון הקודם) מתאימה רק אם `isCompatibleElo(...)` - ר' למטה.
- **`eloFor(String username)`** (חדשה, private) - `accountRepository.currentElo(username)`,
  או `Optional.empty()` אם `username`=null.
- **`isCompatibleElo(Integer searcherElo, Integer candidateElo)`** (חדשה,
  private) - true אם ההפרש ≤100 (`ELO_MATCH_RANGE`), **או אם אחד
  הצדדים (או שניהם) לא ידוע בכלל** (fallback סובלני - נבחר בכוונה: בלי
  זה, כל חיבור אנונימי/בדיקה היה נתקע לגמרי בלי אף אחד להתאים אליו,
  גרוע יותר מהתאמה בלי בדיקת ELO).
- **`matchmakingDeadlines`** (שדה חדש, `Map<String, Long>`) - gameId ←
  רגע (`System.currentTimeMillis()`) שבו פג ה-timeout שלו. נרשם רק
  כש-`resolveMatchmakingGameId` **יוצרת** session חדש (לא כשמצאה match
  קיים - אז נמחק במקום). **זמן-קיר אמיתי בכוונה**, לא שעון-המשחק המדומה
  (`RaelTime`) של `GameSession` - `GameServer` ממילא לא נבדק ביחידה
  (דורש שרת/רשת חיים), וכבר משתמש ב-`System.nanoTime()` ישירות
  ב-`tickAllSessions` הקיימת.
- **`checkMatchmakingTimeout(gameId, session)`** (חדשה, private, נקראת
  מ-`tickAllSessions` על כל session) - אם אין דדליין רשום, יוצאת מיד
  (דילוג זול). אם כבר לא ממתין/ה - רק מנקה את הרישום. אם עדיין ממתין/ה
  וגם עבר הדדליין - שולחת `MatchmakingTimeoutMessage` ישירות לחיבור
  היחיד (`session.connections()`, יש בדיוק אחד במצב הזה), ומנקה את
  הרישום (כדי לא לשלוח שוב בכל טיק).
- **`kfchess.server.MatchmakingTimeoutMessage`** (DTO חדש) - `type="MATCHMAKING_TIMEOUT"`
  + `message` חופשי (מבנה זהה ל-`ErrorMessage`).
- **צד לקוח**:
  - `IncomingMessageSummary.isMatchmakingTimeout` + `describe` - מקביל
    ל-`isRoleAssigned`/ה-case של ROLE_ASSIGNED.
  - `GameClient` - שדה `matchmakingTimeoutMessage` (volatile, אותו דפוס
    בדיוק כמו `assignedGameId`), נחתך ב-`onMessage`.
  - **`NetworkGameWindowMain`** - ה-`Timer` הקיים (אותו Timer שכבר "סוקר"
    הודעות SNAPSHOT) בודק בכל טיק גם את `client.matchmakingTimeoutMessage()`;
    אם לא null - עוצר את עצמו, ומציג `JOptionPane` חוסם עם ההודעה, ואז
    **סוגר את התהליך** (`System.exit(0)`) - **רות אישרה במפורש** את
    ההתנהגות הזו: אין כרגע מסלול "חזרה למסך הבית" בכלל (ברגע שנכנסים
    לחלון המשחק, אין back), וזה עקבי עם `Img`'s `EXIT_ON_CLOSE` הקיים
    ממילא (סגירת החלון הרגילה כבר מסיימת את התהליך).
- **טסטים חדשים**: `GameSessionTest` - 4 טסטים ל-`waitingPlayerUsername`
  (יש username/שני הצדדים מחוברים/אף אחד לא מחובר/מחובר בלי login).
  `IncomingMessageSummaryTest` - 3 טסטים ל-`isMatchmakingTimeout`/`describe`.
  **`GameServer` עדיין בלי טסטים ייחודיים** (דורש שרת/רשת חיים - לא
  השתנה, אותו מצב כמו כל שאר הלוגיקה שם).
- **אימות שבוצע כאן**: איזון סוגריים על כל הקבצים שנגעתי בהם - תקין,
  grep ל-`resolveMatchmakingGameId(` ודא שקריאה אחת בלבד עם החתימה
  החדשה - נקי. **טרם `mvn test` וטרם הרצה ידנית** (כולל בדיקה אמיתית
  של ה-timeout, שדורשת לחכות דקה שלמה) - ר' "מה שנשאר לאמת" למטה.

### תצוגת מידע על המסך - שם חדר / תפקיד / שם משתמש (הסבב הזה)

רות הריצה `mvn test` בהצלחה (141/141) ודיווחה שתי בקשות המשך: (1) חשד
לבאג ניקוד - קפיצה-תפיסה לא נספרת; (2) להוסיף לתצוגת המשחק שם חדר,
תפקיד/צופה, ושם שחקן. לגבי (1): נבדק בקוד לעומק, **לא נמצא נתיב-קוד
תואם** - ב-JUMP בפרויקט הזה כלל אין תפיסה (ר' פירוט בסוף הסעיף) - מחכה
לתיאור-חזרה מדויק מרות לפני שממשיכים בזה. לגבי (2) - הוחלט (2 שאלות
ל-רות): שם החדר בפס עליון **קבוע** לרוחב כל הסצנה (מידע כללי, לא
תלוי-מצב כמו הבאנרים הקיימים); תפקיד+שם משתמש - **רק המידע של הלקוח
עצמו** (לא של היריב/ה) - בפאנל הצד המתאים לשחקן/ית, ובפס העליון לצופה/ה
(אין לצופה/ה "פאנל משלו/ה").

- **`GameClient`** - עד עכשיו פרסרה מהודעת `ROLE_ASSIGNED` רק את ה-gameId
  והתעלמה מ-`role` (שהשרת כן שלח). שדה חדש `assignedRole` (`ClientRole`,
  `volatile`) + פרסור ב-`onMessage` + getter `assignedRole()` - אותו
  דפוס בדיוק כמו `assignedGameId()` הקיים. `ClientRole.valueOf(...)`
  בטוח כי השרת שולח `role.name()` של אותו enum בדיוק (`GameServer.onOpen`).
- **`HomeScreenMain`** - `username` (כבר ידוע כאן מה-`Account`) הועבר
  סוף-סוף גם ל-`NetworkGameWindowMain.launch(...)` - לא הועבר קודם, שימש
  רק לבניית ה-URI. `connect(...)` קיבלה פרמטר `username` חדש (כל שלושת
  נתיבי החיבור - Play/Create/Join - מעבירים אותו).
- **`NetworkGameWindowMain.launch(...)`** - פרמטר שלישי `username`. קוראת
  ל-`client.assignedRole()` (מוכן עד כאן - מגיע באותה הודעה בדיוק כמו
  gameId, ש-`waitForAssignedGameId` כבר ממתינה לה) ומעבירה gameId+role+username
  ל-`GameSceneView` **בבנאי**, לא ב-render() - הם קבועים לכל אורך חיי
  החלון, לא משתנים כמו שאר תוכן ה-snapshot.
- **`GameSceneView`** - בנאי חדש עם roomId/role/username (הישן, בלי
  הפרמטרים האלה, נשאר כ-overload - `NetworkClickHandlerTest` הקיים לא
  נוגע כלל ב-render() אז לא הושפע). `drawRoomHeader(...)` חדשה - פס
  עליון **קבוע** (בניגוד לבאנרים התלויים-מצב הקיימים - ניתוק/המתנה)
  לרוחב **כל** הסצנה (כולל שני הפאנלים, לא רק הלוח) - "Room: XXXXXX",
  ועבור SPECTATOR גם "Spectator: <username>" (ל-WHITE/BLACK המידע הזה
  מוצג בפאנל הצד במקום, לא כאן). קבוע חדש `roomHeaderHeight()` (public
  static) - `NetworkGameWindowMain` *חייב* להשתמש באותו מספר כשהוא
  מקטין את השטח הפנוי ללוח/פאנלים, בדיוק העיקרון שכבר קיים
  ב-`BoardLayoutCalculator` ("חישוב במקום אחד").
- **`SidePanelView.draw(...)`** - שני פרמטרים חדשים: `startY` (כדי
  שהפאנל יתחיל מתחת לפס שם-החדר, לא מ-0) ו-`isLocalPlayer`+`username`
  (מוסיפים "(You: ruth)" לתוך שורת הכותרת הקיימת - לא שורה נפרדת, כדי
  לא לשנות שום מיקום/גובה אחר בפאנל). קריאה אחת בלבד מכל הפרויקט
  (מ-`GameSceneView.render`) - אין overload ישן, בניגוד ל-DTOs שנשלחים
  ברשת.
- **`NetworkGameWindowMain`** - פונקציה חדשה `computeBoardLayout(...)`
  מרכזת את חישוב ה-`BoardLayout` (נקראת גם מ-`renderFrame` וגם
  מ-`handleClick`, בדיוק העיקרון של `BoardLayoutCalculator`): מחסירה את
  `roomHeaderHeight()` מהגובה הפנוי *לפני* `BoardLayoutCalculator.computeLayout`
  (כדי שהלוח לא ייחשב גדול מדי ו"יגלוש" מתחת לפס - זה גם היה גורם
  ל-`Img.drawOn` לזרוק `IllegalArgumentException` בפועל), ומוסיפה את
  אותו גובה בחזרה ל-`offsetY` המתקבל (כדי שהלוח יתחיל בפועל מתחת לפס).
  לא נגעתי ב-`BoardLayoutCalculator` עצמה (מחלקה טהורה, נבדקת) - כל
  ה"תוספת" נשארת ב-`NetworkGameWindowMain`.
- **טסטים חדשים**: `GameClientTest` (חדש - `GameClient` לא היה לו
  טסטים ייחודיים קודם; מתמקד ב-`assignedRole()`, נבנה עם URI דמה בלי
  `connectBlocking()`, בדיוק כמו `RecordingGameClient` הקיים - `onMessage()`
  לא נוגעת ברשת בכלל). `GameSceneView`/`SidePanelView`/`NetworkGameWindowMain`
  נשארים בלי טסטים ייחודיים (ציור/Swing, כמו קודם).
- **אימות שבוצע כאן**: איזון סוגריים על כל הקבצים שנגעתי בהם - תקין.
  grep להפניות ישנות לחתימות שהשתנו (`new GameSceneView(`,
  `NetworkGameWindowMain.launch(`, `sidePanelView.draw(`, `connect(`) -
  כולן עודכנו, רק ה-overload המכוון ב-`NetworkClickHandlerTest` נשאר על
  הישן בכוונה. חישבתי ידנית (בלי `mvn`) את הגיאומטריה במקרה הרגיל -
  תקין, הלוח לא גולש. **טרם `mvn test` וטרם הרצה ידנית** - ר' "מה שנשאר
  לאמת" למטה.
- **בעיית הניקוד (קפיצה-תפיסה) - תוארה מדויק בהמשך ותוקנה** (ר' סעיף
  ייעודי בהמשך הקובץ, אחרי סעיף העיצוב) - התיאור הראשוני היה עמום מדי
  לאתר; רות תיארה מדויק יותר: "כלי בא לאכול כלי אחר, הכלי המותקף קופץ
  ואוכל את התוקף" - זה בדיוק תרחיש "לכידה באוויר"
  (`captureFailsAgainstJumpingDefender`), לא ה-JUMP הרגיל.

### עיצוב יותר יפה לתצוגת המידע (הסבב הזה)

רות הריצה את הסבב הקודם ושלחה צילום מסך: הפס העליון החדש (שם החדר) הציג
UUID ענק ("Room: match-816a33df-6e2a-4772-b3e7-09c6768d4150") שגלש כמעט
לכל רוחב המסך - זה בדיוק מה שהפך אותו למכוער. גם הטקסט "(You: username)"
בתוך כותרת הפאנל וגם הסטייל הכללי (פונט/צבעים שטוחים) צוינו כבעייתיים.
לפני מימוש בקוד - **הוצג ל-רות mockup חזותי (HTML, לא קוד אמיתי)** דרך
כלי ה-visualize כדי לתאם עיצוב/צבעים לפני שמשקיעים בקוד Swing בפועל -
אושר ("ממש יפה"). רות שאלה במפורש אם זה משנה את **תמונות הכלים עצמן**
(sprites/board.png) - **לא**: השינוי מוגבל לשלושה קבצים (`GameSceneView`,
`SidePanelView`, `Img`) - לא נוגע ב-`BoardView`/`SnapshotFactory`/תמונות
הכלים בכלל.

- **`Img.fillRoundRect(...)`** (חדשה) - כמו `fillRect` הקיימת, אבל עם
  פינות מעוגלות (`Graphics2D.fillRoundRect`) - נחוצה לתג "You" (למטה).
  `fillRect`/`drawRect` הקיימות נשארות ללא שינוי (עדיין רלוונטיות למקומות
  שרוצים פינות חדות - למשל גבול הלוח/הפאנלים עצמם).
- **`GameSceneView.shortRoomId(String gameId)`** (חדשה, `public static`,
  טהורה לגמרי - בלי Img/גרפיקה) - זו **הבעיה המרכזית** מהצילום: מזהי
  matchmaking (`"match-<uuid>"`, ר' `GameServer.resolveMatchmakingGameId`)
  ארוכים בטירוף לתצוגה. מציגה רק 8 התווים הראשונים של ה-UUID **בלי**
  הקידומת "match-" - "Room: 816a33df" (בדיוק כמו שרות ביקשה). **קודי
  Create/Join (הקוד הקצר שצריך למסור לחברה, ר' `RoomIdGenerator`) לא
  נוגעים בהם בכלל** - קיצור שלהם היה שובר את הפיצ'ר עצמו (השחקנית השנייה
  חייבת להקליד את הקוד המדויק). נבדקת ב-`GameSceneViewTest` חדש (5
  טסטים) - הראשונה שנבדקת ישירות מהמחלקה הזו (שאר `GameSceneView` נשארת
  בלי טסטים ייחודיים, ציור/Swing כמו קודם).
- **`GameSceneView.drawRoomHeader(...)`** - שונתה מקריאת `drawText` אחת
  בצבע אחיד לשלוש קריאות נפרדות ("Room " לבן, מזהה החדר המקוצר בגוון
  זהב-עמום `ROOM_ID_ACCENT`, ול-SPECTATOR גם "Spectator: username" בגוון
  עמום `ROOM_HEADER_MUTED`) - `Img.drawText` לא תומך בכמה צבעים במחרוזת
  אחת, אז מחשבת `textWidth` לכל קטע מראש (למרכז את הקבוצה כולה) ואז
  מציירת ברצף עם "סמן" X שמתקדם. `ROOM_HEADER_BACKGROUND` עודכן מאפור-
  כחלחל שטוח לגוון חום-כהה, כדי להתאים לגוון הזהב של המזהה.
- **`SidePanelView`** - שני שינויים:
  1. **שתי "ערכות נושא" נפרדות** (`record Theme` פרטי חדש - background/
     border/headerText/moveText/badgeBackground/badgeText) במקום צבע
     אחיד ששני הפאנלים חלקו עד עכשיו - `WHITE_THEME` בהיר-חם, `BLACK_THEME`
     כהה - נבחרת לפי הפרמטר `color` שכבר קיים ב-`draw()`, בלי פרמטר
     חדש. זה בדיוק "עיצוב שונה לכל אזור" שרות ביקשה.
  2. **"You: username" עבר משורת הכותרת לתג/פילה נפרדת** (`drawYouBadge`,
     חדשה, פרטית) - מלבן מעוגל (`Img.fillRoundRect`) מתחת לכותרת, ברוחב
     מחושב מהטקסט עצמו (כמו שכפתור ה-Restart כבר עושה). השטח מתחת
     לכותרת (`BADGE_TOP_Y`) **שמור קבוע** גם כשהתג לא מוצג (יריב/ה) - כדי
     ששני הפאנלים יישארו מיושרים בדיוק זה מול זה, בלי קשר למי מהם "שלי"
     כרגע. `SCORE_Y`/`MOVES_TITLE_Y`/`MOVES_START_Y` זזו כולם ~10px למטה
     כדי לפנות מקום לשורת התג.
- **אימות שבוצע כאן**: איזון סוגריים על כל הקבצים שנגעתי בהם - תקין.
  grep לקריאות `sidePanelView.draw(` - רק שתיים, שתיהן ב-`GameSceneView`
  (כבר מתאימות לחתימה) - נקי. **טרם `mvn test` וטרם הרצה ידנית** - ר' "מה
  שנשאר לאמת" למטה.

### תיקון באג ניקוד - "לכידה באוויר" (הסבב הזה)

רות תיארה מדויק: "כשכלי בא לאכול כלי אחר ובסוף הכלי המותקף קופץ ואוכל
את התוקף הוא לא מקבל נקודות". זה בדיוק תרחיש `GameEngine.
captureFailsAgainstJumpingDefender` ("לכידה באוויר", ר' התיעוד למעלה
בתחילת הקובץ): כלי א' זז לתפוס את כלי ב', אבל ב' נמצא/ת במצב JUMPING
(קפיצה, לא תנועה) - א' "מתאדה" (נמחק מהלוח) ו-ב' נשאר/ת שלם/ה במקום.
**מנקודת המבט של ב' זו תפיסה אמיתית** (א' נמחק בפועל) - אבל הקוד הקודם
לא זיקף נקודות לאף אחד, "כאילו לא קרה כלום".

- **`MoveHistory.recordCounterCapture(defender, attacker, from, to)`**
  (חדשה, מחליפה את `recordFailedCapture` הקודמת - שלא היה לה קורא נוסף
  בכלל, לא היה טעם להשאיר את שתיהן) - שתי שורות ביומן: אחת לתוקף/ת (המהלך
  *שלו/ה* נכשל, סימון "?!" בדיוק כמו קודם) ואחת למגן/ת (תפיסה-נגדית
  מוצלחת, סימון "(jump)" חדש) - **ורק המגן/ת מקבל/ת נקודות** (`scores.merge`
  לפי `attacker.kind().value()`, בדיוק אותה נוסחה כמו ב-`recordMove`
  הרגילה, רק שהצדדים הפוכים - "מי תפס את מי" הוא המגן/ת הפעם, לא
  התוקף/ת). `SoundEvent` שונה מ-`ILLEGAL` ל-`CAPTURE` - תפיסה אמיתית
  קרתה, גם אם לא בכיוון שהתוקף/ת תכנן/ה.
- **`GameEngine.captureFailsAgainstJumpingDefender(motion, movingPiece, defendingPiece)`**
  - קיבלה פרמטר שלישי חדש (`defendingPiece`, את הכלי המגן עצמו - קודם
    הפונקציה קיבלה רק את התוקף) - כדי ש-`recordCounterCapture` תדע *למי*
    לזקוף את הניקוד. קריאה יחידה לפונקציה הזו (מ-`completeMotion`)
    עודכנה בהתאם.
- **מה שלא השתנה בכוונה**: תפיסה רגילה (המגן/ת *לא* קופץ/ת) עדיין
  עוברת ב-`MoveHistory.recordMove()` בדיוק כמו קודם, בלי שום שינוי - זה
  היה, ונשאר, המסלול הנכון והיחיד לתפיסות "רגילות". ה-JUMP עצמו
  (`GameEngine.handleJump`/`NetworkActions.handleJump`, ר' התיעוד
  המקורי למעלה) גם הוא לא השתנה - עדיין לא מזיז/תופס שום דבר בעצמו,
  רק מסמן `PieceState.JUMPING`; זה ה-`completeMotion` של הצד השני
  (התוקף/ת) שמגלה את המצב הזה ומפעיל עכשיו את הניקוד.
- **טסטים חדשים**: `GameEngineTest` (קובץ חדש - אין `GameEngineTest`
  קיים, `RuleEngineTest` בודק חוקיות-מהלכים טהורה בלבד, לא את
  `GameEngine` עצמו) - 3 טסטים: המגן/ת מקבל/ת את ערך הכלי של התוקף/ת
  בניקוד; התוקף/ת נמחק/ת מהלוח והמגן/ת נשאר/ת שלם/ה; ורגרסיה מפורשת -
  תפיסה רגילה (בלי קפיצה) עדיין מזקיפה נקודות לתוקף/ת כמו קודם, לא
  השתנתה. בונה `GameEngine` ישירות (`new GameEngine(new Game(board), new
  RuleEngine(), new RaelTime(), new EventBus())`) עם `RaelTime` מדומה
  (`handleWait(1000)` "מקפיץ" את הזמן בלי `Thread.sleep` אמיתי) - אותה
  שיטה בדיוק כמו `GameSessionTest`.
- **אימות שבוצע כאן**: איזון סוגריים על `GameEngine.java`/`MoveHistory.java`/
  `GameEngineTest.java` - תקין. grep ל-`recordFailedCapture` (השם הישן) -
  אין אף הפניה שנשארה, נקי. **טרם `mvn test` וטרם הרצה ידנית** - ר' "מה
  שנשאר לאמת" למטה.

## מה שנשאר לאמת (רות - עדיין לא נעשה)

שוב: אין לי `javac`/`mvn` בסביבה שלי (אין root, אין גישת רשת להוריד
JDK/Maven) - בדקתי רק איזון סוגריים + חיפוש הפניות שבורות לכל קובץ
שנגעתי בו/נמחק, ואת נוסחת ה-ELO אימתתי גם בחישוב Python נפרד (לא רק
"נראה הגיוני") - אבל **זה לא תחליף ל-`mvn test` אמיתי**. צריך:

1. ~~`mvn clean test`~~ - **ירוק לגמרי, אושר ע"י רות בכל השלבים** (כולל
   הצבירה המלאה עד הלוגים - חדרים, תיקון Play, חסימת מהלכים, באנר
   Waiting, וגם `FileLoggerTest` - "הרצתי טסטים הרצתי חלוניות הכל
   עובד"). הרגרסיה שנמצאה באמצע (`tick_clickThenLegalTarget_movesPieceIntoTransit`/
   `...snapshotIncludesMatchingMotion`) תוקנה ואומתה מחדש.
2. **הרצה ידנית מקצה לקצה של ELO (עדיין לא בוצעה מאז שלב 4 Part B)**:
   - Run על `ServerMain`.
   - **Register שני חשבונות שונים** דרך `LoginScreenMain` (למשל
     "ruth" ו-"dani") - חשוב שיהיו **חשבונות שונים ולא אותו אחד פעמיים**,
     אחרת ה-ELO מדלג בכוונה (ר' Part B למעלה, "בדיקה עצמית").
   - שני חלונות `LoginScreenMain` (Allow multiple instances) - כל אחד
     מתחבר לאותו room עם החשבון שלו.
   - לשחק עד לכידת מלך (סיום משחק אמיתי).
   - לבדוק ב-`kfchess.db` (או ע"י Login מחדש ובדיקת "ELO" בתווית) ששני
     החשבונות השתנו: המנצח/ת עלה, המפסיד/ה ירד, בסכום שהגיוני ל-K=32
     (למשל אם שני החשבונות התחילו ב-1200, אמור להיות 1216/1184).
   - **רות אישרה שזה כבר עבד בפועל** (ראתה ציון משתנה) - הבדיקה הזו
     כאן היא בעיקר לחזור ולוודא שאין רגרסיה אחרי שינויי ה-Restart.
3. **הרצה ידנית של Restart (חדש, טרם נבדק בפועל - זה היה הבאג המקורי
   שרות דיווחה)**:
   - לשחק עד game over עם שני חלונות (WHITE+BLACK).
   - ללחוץ Restart **רק בצד אחד** - לוודא: הכפתור עצמו משנה טקסט ל-
     "Waiting..." (וגם הכותרת ל-"Waiting for opponent...") אצל **שני**
     הצדדים (לא רק אצל מי שלחץ), אבל הלוח **לא** מתאפס.
   - ללחוץ Restart גם בצד השני - לוודא שהלוח **כן** מתאפס (חוזר למצב
     פתיחה) אצל שניהם, וה"Waiting" נעלם.
4. **הרצה ידנית של ניתוק/auto-resign** - **תרחיש ה-timeout (המתנה 20
   שניות בלי חיבור מחדש, היריב מוכרז כמנצח) כבר אומת ועובד, אושר ע"י
   רות.** עדיין לא אומתו ידנית שני תרחישים נוספים:
   - **reconnect בתוך חלון החסד**: לנתק צד (סגירת חלון), **לפני** שעברו
     20 שניות לפתוח `LoginScreenMain` חדש, Login עם **אותו username
     בדיוק**, להתחבר לאותו room - לוודא: חוזר/ת בתור **אותו צבע** בדיוק
     שהיה לו/ה קודם, הבאנר נעלם אצל שני הצדדים, והמשחק ממשיך רגיל.
   - **"לא נגנב"**: תוך כדי חלון החסד, לנסות לפתוח `LoginScreenMain`
     **שלישי** עם username **אחר** ולהתחבר לאותו room - לוודא שנכנס/ת
     כ-SPECTATOR (לא "תופס/ת" את הצבע השמור).
5. **הרצה ידנית של matchmaking (חדש, טרם נבדק בפועל כלל - הסבב הזה)**:
   - Run על `ServerMain`, שני חלונות `LoginScreenMain` (חשבונות שונים).
   - **בשני החלונות**: להשאיר את שדה ה-room ריק/כברירת מחדל, וללחוץ
     **Skip** (לא Connect) בשניהם - לוודא: שני החלונות מתחברים **לאותו
     משחק בדיוק** (אחד/ת WHITE, השני/ה BLACK), הלוחות מסונכרים.
   - **תרחיש "3 שחקנים"** (זה בדיוק מה שדיווחת): לחלון ראשון ללחוץ Skip
     (נפתח משחק חדש, ממתין/ה). לחלון שני **גם** ללחוץ Skip - אמור להצטרף
     לאותו משחק (BLACK). לחלון **שלישי** ללחוץ Skip שוב - לוודא שהוא/היא
     **לא** נכנס/ת כ-SPECTATOR למשחק הראשון, אלא פותח/ת משחק **חדש**
     ומחכה/ת (בדיוק הבעיה שדיווחת - אמורה להיפתר).
   - לוודא ש-Connect (עם שם room ידני) עדיין עובד בדיוק כמו קודם, בלי
     שינוי - זה משהו ש-matchmaking לא אמור לגעת בו בכלל.
6. ✅ **אושר ע"י רות** - הרצה ידנית של חדרים (Create/Join/Cancel):
   - Run על `ServerMain`, `LoginScreenMain` (Allow multiple instances).
   - חלון ראשון: **Room...** → **Create** - לוודא: מסך המשחק נפתח מיד
     (כ-WHITE), וכותרת החלון מציגה `KFChess - Room: XXXXXX` (קוד בן 6
     תווים) - **זה ה-ID שצריך למסור לשחקן/ית השני/ה**.
   - חלון שני: **Room...** → מקלידים בתיבה בדיוק את הקוד מהחלון הראשון →
     **Join** - לוודא: נכנס/ת לאותו משחק כ-BLACK, כותרת החלון זהה
     (אותו קוד), הלוחות מסונכרנים.
   - חלון שלישי: אותו תהליך (Join עם אותו קוד) - לוודא שנכנס/ת כ-SPECTATOR.
   - לוודא ש-**Cancel** בדיאלוג פשוט סוגר אותו בלי לחבר כלום.
   - לוודא ש-**Join עם תיבה ריקה** מציג הודעת שגיאה מקומית בדיאלוג
     ("Enter a room ID to join") ולא מתחבר בכלל.
   - לוודא ש-**Play** (השם החדש לכפתור "Skip" לשעבר - הלוגיקה לא שונתה)
     ו-Join-לפי-קוד לא "מתנגשים" - עדיין ניתן להשתמש בשניהם לסירוגין.
7. ✅ **אושר ע"י רות** - הרצה ידנית של התיקון ל-Play/חדרים פרטיים (זה
   בדיוק התרחיש שרות דיווחה):
   - חלון ראשון: Room... → Create (חדר פרטי, ממתין).
   - חלון שני: **Play** (לא Join) - לוודא ש-**לא** מצטרף/ת לחדר הפרטי -
     אמור/ה לפתוח matchmaking session חדש ולחכות שם.
   - לוודא ש-Play עדיין עובד רגיל בין שני לקוחות ששניהם לחצו Play
     (התרחיש שכבר אומת בעבר - בלי רגרסיה).
8. ✅ **אושר ע"י רות** - הרצה ידנית של "לא ניתן להתחיל לשחק בזמן המתנה":
   - חלון ראשון: Room... → Create, נכנס/ת כ-WHITE, לבד/ה בחדר.
   - לנסות ללחוץ/לבחור כלי - לוודא ש**שום דבר לא קורה** (אין הדגשה
     ויזואלית של הכלי, אין תזוזה).
   - חלון שני: Room... → Join עם אותו קוד - נכנס/ת כ-BLACK.
   - עכשיו לנסות שוב ללחוץ על כלי בחלון הראשון - לוודא שהפעם **כן
     עובד** רגיל (בחירה + תזוזה).
9. ✅ **אושר ע"י רות** - הרצה ידנית של הבאנר "Waiting for an opponent...":
   - חלון ראשון: Room... → Create - לוודא שמופיע **מיד** פס כחול בראש
     הלוח עם הטקסט "Waiting for an opponent to join...".
   - חלון שני: Room... → Join עם אותו קוד - לוודא שהפס **נעלם** משני
     החלונות ברגע שה-BLACK מצטרף/ת בפועל.
   - לוודא שהפס הכחול **לא** מופיע יחד עם פס הניתוק הכתום (הם לא אמורים
     להופיע בו-זמנית לעולם - ר' התיעוד למעלה).
10. ✅ **אושר ע"י רות** - הרצה ידנית של הלוגים:
    - Run על `ServerMain` - לוודא שנוצרה תיקייה `logs/` ובתוכה קובץ
      `server_<תאריך-שעה>.log` עם שורת "GameServer started on port...".
    - Login + Room→Create בחלון אחד - לוודא שנוצר גם `client_<...>.log`
      (בתיקיית העבודה שממנה IntelliJ מריצה את הלקוח - יכולה להיות
      תיקייה אחרת מזו של השרת, תלוי בקונפיגורציה), ושבקובץ ה-server
      נוספה שורה "Connection opened via Create...".
    - לשחק כמה מהלכים - לוודא שב-client log נוספות שורות "Sending
      CLICK/JUMP...", וב-server log **לא** מצטברות אלפי שורות SNAPSHOT
      (רק ROLE_ASSIGNED/שגיאות בצד הלקוח - זה מכוון).
    - לסגור את חלון הלקוח - לוודא שב-server log נוספה שורה "Connection
      closed...".
11. ✅ **אושר ע"י רות (חלקית)** - הרצה ידנית של תיקון Play:
    - **timeout של דקה**: **אומת ועובד** - רות הריצה חלון בודד, לחצה
      Play, אחרי 60 שניות נפתח popup והחלון נסגר. בדיוק לפי הדרישה.
    - **התאמת ELO** ו**Create/Join שלא מושפעים** - **עדיין לא אומתו
      במפורש** (דורש שני חשבונות עם ELO רחוק/קרוב בכוונה) - לא חוסם,
      אבל כדאי לוודא בהזדמנות.
12. תזכורות מסבבים קודמים שעדיין רלוונטיות: קבצים לא-קשורים שכבר
   מופיעים כ-modified ב-`git status` (line-ending, לא תוכן - לא נגעתי
   בהם), ותיקיית `src/main/java/kfchess/.claude/` וקובץ `kfchess.db`
   שלא יצרתי (untracked, לא ב-git add המוצע).
13. **הרצה ידנית של תצוגת המידע (שם חדר/תפקיד/שם משתמש, הסבב הזה, טרם
    נבדק בפועל כלל)**:
    - Run על `ServerMain`, `LoginScreenMain` (Allow multiple instances).
    - חלון ראשון: Room... → Create - לוודא שמופיע פס אפור-כחלחל **קבוע**
      לרוחב כל החלון (כולל שני הפאנלים) עם "Room: XXXXXX" - **תמיד**
      גלוי, לא רק בכותרת החלון כמו קודם. לוודא שהלוח עצמו לא "חתוך"/גולש
      מתחת לפס (רק קטן קצת יותר משהיה, כדי לפנות לו מקום).
    - לוודא שבפאנל הצד השמאלי (White) מופיע "White (You: <username שלי>)"
      (החלון הראשון הוא WHITE).
    - חלון שני: Room... → Join עם אותו קוד - לוודא שבפאנל הימני (Black)
      מופיע "Black (You: <username של החלון השני>)", ושבחלון **הראשון**
      הפאנל השמאלי עדיין מציג "(You: ...)" (לא "התחלף" בטעות ליריב).
    - חלון שלישי: Join עם אותו קוד (SPECTATOR) - לוודא שהפס העליון מציג
      גם "Spectator: <username שלו/ה>" (בנוסף ל-Room), ושאף אחד משני
      הפאנלים **לא** מציג "(You: ...)" עבורו/ה (אין "פאנל שלו/ה").
    - לוודא שקליקים/Restart/הבאנרים הקיימים (ניתוק/המתנה) עדיין עובדים
      בדיוק כמו קודם - השינוי הזה לא אמור לשבור אף אחד מהם.
14. **הרצה ידנית של תיקון הניקוד ("לכידה באוויר", הסבב הזה, טרם נבדק
    בפועל כלל)**:
    - שני חלונות, שני חשבונות. צד אחד (למשל BLACK) קליק-ימני על כלי
      שלו/ה כדי לגרום לו לקפוץ (מסגרת/אנימציית קפיצה אמורה להיראות).
    - הצד השני (WHITE) קליק על כלי שיכול לזוז/לתפוס את הכלי שקופץ,
      ולפני שהקפיצה נגמרת - קליק על המשבצת של הכלי הקופץ (תפיסה).
    - לוודא: הכלי **התוקף** (WHITE) נעלם מהלוח, הכלי הקופץ (BLACK) נשאר
      במקומו בשלמותו, וה-Score **של BLACK** (המגן/ת) עולה בערך הכלי
      שנעלם (למשל 5 אם זה היה צריח). Score של WHITE **לא** משתנה.
    - לוודא שתפיסה רגילה (בלי קפיצה בכלל) עדיין עובדת כרגיל - לא נשברה.
    - **הערה חשובה לסבב AI עתידי**: בסבב הזה רות ביקשה במפורש **לא
      לתקן** שני דברים ידועים בעיצוב (ר' סעיף העיצוב למעלה) - הבאנרים
      הכחול/כתום (ניתוק/המתנה) עדיין שקופים-למחצה (לא הפכתי אותם ל-flat),
      וייתכן שיש באג ב-`Img.drawText` שמציג טקסט עברי הפוך (`drawString`
      גולמי, לא `TextLayout`) - **שניהם ידועים, לא תוקנו בכוונה, אל תניחי
      שהם "נשמטו"** - רק אם רות תבקש את זה במפורש בעתיד.
15. **הרצה ידנית של העיצוב המחודש (הסבב הזה, טרם נבדק בפועל כלל)**:
    - חלון ראשון: **Play** (לא Room→Create) - לוודא שהפס העליון מציג
      "Room " בלבן ואחריו קוד קצר בגוון זהב-עמום (8 תווים, **לא** UUID
      ענק) - למשל "Room 816a33df", לא "Room match-816a33df-6e2a-...".
    - חלון ראשון בנפרד: Room... → **Create** - לוודא שהקוד הקצר (6
      תווים) מוצג **מלא** בפס (לא מקוצר עוד יותר) - זה הקוד שצריך למסור
      לחברה כדי שתצטרף.
    - לוודא שבפאנל השמאלי (White) יש רקע בהיר-חם (שונה מהאפור הקודם) עם
      "פילה" קטנה מעוגלת "You: <username>" מתחת לכותרת "White" (לא
      בתוך השורה עצמה).
    - חלון שני: Join עם אותו קוד - לוודא שהפאנל הימני (Black) בעל רקע
      כהה נפרד (לא אותו רקע כמו White), עם הפילה "You: ..." שלו/ה.
    - לוודא שבשני החלונות, הפאנל של **היריב/ה** (לא שלי) לא מציג פילה
      בכלל, אבל הניקוד/רשימת המהלכים עדיין באותו מקום בדיוק בשני
      הפאנלים (לא "זזים" יחסית אחד לשני).
    - חלון שלישי (SPECTATOR): לוודא שהפס העליון מציג גם "Spectator:
      username" בגוון עמום יותר, ואף פאנל לא מציג פילה בשבילו/ה.

## פקודת commit מוצעת (לא הרצתי - רק `git status`/`git diff` לבדיקה)

**הערה חשובה על ה-commit הבא**: כמה מהקבצים שנוגעים בשלב 5 חלק 2
(`GameSession.java`, `GameServer.java`) כבר `git add`-ed חלקית (staged)
בגלל שינוי שם החבילה שרות עשתה (ר' "הערה טכנית חשובה" למעלה) - עוד
לפני שאני נגעתי בהם הסבב הזה. **פקודת ה-`git add` למטה, כשתרוצי אותה,
תכלול אוטומטית גם את שינוי שם החבילה** לאותם קבצים (אי אפשר להפריד
"רק את השורות שלי" מ-"רק את שינוי השם" באותו קובץ) - כנראה בסדר (השם
החדש צריך להיכנס למשהו בסוף בכל מקרה), רק מציינת כדי שלא תופתעי מגודל
ה-diff בקומיט.

Part B (אם עדיין לא בוצע commit נפרד לו):
```
git add src/main/java/kfchess/HomeScreenMain.java src/main/java/kfchess/LoginScreenMain.java src/main/java/kfchess/account/ src/main/java/kfchess/net/server/ src/test/java/texttests/EloCalculatorTest.java src/test/java/texttests/GameIdResolverTest.java src/test/java/texttests/UsernameResolverTest.java src/test/java/texttests/HomeScreenMainTest.java src/test/java/texttests/SqliteAccountRepositoryTest.java PROGRESS.md
git commit -m "Stage 4 Part B: wire logged-in username into the network protocol and auto-update ELO when a game ends"
```

פיצ'ר Restart (הסבב הזה):
```
git add src/main/java/kfchess/net/ClientCommandType.java src/main/java/kfchess/net/ClientCommand.java src/main/java/kfchess/net/SnapshotMessage.java src/main/java/kfchess/net/client/GameClient.java src/main/java/kfchess/net/client/NetworkClickHandler.java src/main/java/kfchess/net/client/IncomingSnapshot.java src/main/java/kfchess/net/client/ClientSnapshotReconstructor.java src/main/java/kfchess/net/server/GameSession.java src/main/java/kfchess/engine/snapshot/SnapshotFactory.java src/main/java/kfchess/engine/snapshot/GameSnapshot.java src/main/java/kfchess/view/GameSceneView.java src/main/java/kfchess/NetworkGameWindowMain.java src/test/java/texttests/ClientCommandTest.java src/test/java/texttests/GameSessionTest.java src/test/java/texttests/NetworkClickHandlerTest.java src/test/java/texttests/RecordingGameClient.java PROGRESS.md
git commit -m "Add mutual Restart voting (both sides must click) with a Waiting for opponent visual cue"
```

שלב 5, חלק 1 - ניתוק/auto-resign - **בוצע commit בפועל ע"י רות (f43e467)**:
```
git add src/main/java/kfchess/engine/GameEngine.java src/main/java/kfchess/net/server/GameSession.java src/main/java/kfchess/net/server/GameServer.java src/main/java/kfchess/net/SnapshotMessage.java src/main/java/kfchess/net/client/IncomingSnapshot.java src/main/java/kfchess/net/client/ClientSnapshotReconstructor.java src/main/java/kfchess/engine/snapshot/SnapshotFactory.java src/main/java/kfchess/engine/snapshot/GameSnapshot.java src/main/java/kfchess/view/GameSceneView.java src/main/java/kfchess/NetworkGameWindowMain.java src/test/java/texttests/GameSessionTest.java src/test/java/texttests/MessageDtoTest.java src/test/java/texttests/ClientSnapshotReconstructorTest.java PROGRESS.md
git commit -m "Stage 5 part 1: auto-resign on disconnect with a 20s reconnect grace window"
```

שלב 5, חלק 2 - Matchmaking (הסבב הזה, **טרם אומת ידנית/mvn test - ר' "מה שנשאר לאמת"**, ור' ההערה למעלה על שינוי שם החבילה שיכנס יחד עם זה):
```
git add src/main/java/kfchess/HomeScreenMain.java src/main/java/kfchess/server/server/GameSession.java src/main/java/kfchess/server/server/GameServer.java src/main/java/kfchess/server/server/MatchmakingResolver.java src/test/java/texttests/GameSessionTest.java src/test/java/texttests/HomeScreenMainTest.java src/test/java/texttests/MatchmakingResolverTest.java PROGRESS.md
git commit -m "Stage 5 part 2: random matchmaking via a Skip button (find-or-create a waiting game)"
```

שלב 6, חלק 1 - חדרים: Create/Join/Cancel (הסבב הזה, **טרם אומת ידנית/mvn test - ר' "מה שנשאר לאמת"**):
```
git add src/main/java/kfchess/HomeScreenMain.java src/main/java/kfchess/NetworkGameWindowMain.java src/main/java/kfchess/view/Img.java src/main/java/kfchess/server/server/GameServer.java src/main/java/kfchess/server/server/CreateRoomResolver.java src/main/java/kfchess/server/server/RoomIdGenerator.java src/main/java/kfchess/server/client/GameClient.java src/main/java/kfchess/server/client/IncomingMessageSummary.java src/test/java/texttests/HomeScreenMainTest.java src/test/java/texttests/CreateRoomResolverTest.java src/test/java/texttests/RoomIdGeneratorTest.java src/test/java/texttests/IncomingMessageSummaryTest.java PROGRESS.md
git commit -m "Stage 6 part 1: real rooms via a Room dialog (Create/Join/Cancel), room id shown in the window title"
```

**הערה על הודעות commit מכאן ואילך**: רות ביקשה שהודעות ה-commit יכללו
רק הסבר ופרוט של מה השתנה ולמה, בלי לתייג אותן לפי "שלב X חלק Y" - ר'
הבלוק הבא כדוגמה לפורמט החדש.

תיקון - Play לא אמור להצטרף לחדרים פרטיים (הסבב הזה, **טרם אומת ידנית - ר' "מה שנשאר לאמת" סעיף 7**):
```
git add src/main/java/kfchess/server/server/GameServer.java PROGRESS.md
git commit -m "Restrict Play matchmaking to sessions created via Play itself, so it can no longer join a private room opened via Create just because that room's creator is also waiting for someone"
```

מניעת מהלכים בזמן המתנה ליריב (הסבב הזה, **טרם אומת ידנית/mvn test - ר' "מה שנשאר לאמת" סעיף 8**):
```
git add src/main/java/kfchess/server/server/GameSession.java src/test/java/texttests/GameSessionTest.java PROGRESS.md
git commit -m "Ignore CLICK/JUMP commands from a lone connected player until an opponent actually joins, reusing the existing isWaitingForOpponent check so a solo player can no longer move pieces before anyone else is there to play against"
```

באנר "Waiting for an opponent..." (הסבב הזה, **טרם אומת ידנית/mvn test - ר' "מה שנשאר לאמת" סעיף 9**):
```
git add src/main/java/kfchess/server/SnapshotMessage.java src/main/java/kfchess/server/client/IncomingSnapshot.java src/main/java/kfchess/server/client/ClientSnapshotReconstructor.java src/main/java/kfchess/engine/snapshot/GameSnapshot.java src/main/java/kfchess/engine/snapshot/SnapshotFactory.java src/main/java/kfchess/server/server/GameSession.java src/main/java/kfchess/NetworkGameWindowMain.java src/main/java/kfchess/view/GameSceneView.java src/test/java/texttests/GameSessionTest.java src/test/java/texttests/MessageDtoTest.java src/test/java/texttests/ClientSnapshotReconstructorTest.java PROGRESS.md
git commit -m "Show a blue banner reading Waiting for an opponent to join... on the board while a lone player waits, by threading a new waitingForOpponent flag through the whole snapshot pipeline from GameSession down to GameSceneView, the same way disconnectSecondsRemaining already travels"
```

תיקון רגרסיה שרות מצאה ע"י `mvn test` (הסבב הזה):
```
git add src/test/java/texttests/GameSessionTest.java PROGRESS.md
git commit -m "Fix two pre-existing move tests that only connected a lone WHITE player and broke once solo moves got blocked while waiting for an opponent, by also connecting a BLACK player so the game counts as started"
```

לוגים - שלב 6 חלק 2 (הסבב הזה, **טרם אומת ידנית/mvn test - ר' "מה שנשאר לאמת" סעיף 10**):
```
git add src/main/java/kfchess/logging/FileLogger.java src/main/java/kfchess/server/server/GameServer.java src/main/java/kfchess/server/client/GameClient.java src/test/java/texttests/FileLoggerTest.java PROGRESS.md
git commit -m "Log server and client activity to a fresh timestamped text file per run under logs/, covering connections, disconnects, errors and commands sent, separate from the existing in-game move log"
```

תיקון Play - ELO ±100 / timeout של דקה / הודעת "לא נמצא" (הסבב הזה, **טרם אומת ידנית - ר' "מה שנשאר לאמת" סעיף 11**):
```
git add src/main/java/kfchess/server/server/GameSession.java src/main/java/kfchess/server/server/GameServer.java src/main/java/kfchess/server/MatchmakingTimeoutMessage.java src/main/java/kfchess/server/client/IncomingMessageSummary.java src/main/java/kfchess/server/client/GameClient.java src/main/java/kfchess/NetworkGameWindowMain.java src/test/java/texttests/GameSessionTest.java src/test/java/texttests/IncomingMessageSummaryTest.java PROGRESS.md
git commit -m "Restrict Play matchmaking to opponents within 100 ELO of the searcher, falling back to matching regardless when either side's ELO is unknown, and time out a lone waiting player after one minute with a popup telling them no match was found before closing the window"
```

תצוגת מידע על המסך - שם חדר / תפקיד / שם משתמש (הסבב הזה, **טרם אומת ידנית - ר' "מה שנשאר לאמת" סעיף 13**):
```
git add src/main/java/kfchess/server/client/GameClient.java src/main/java/kfchess/HomeScreenMain.java src/main/java/kfchess/NetworkGameWindowMain.java src/main/java/kfchess/view/GameSceneView.java src/main/java/kfchess/view/SidePanelView.java src/test/java/texttests/GameClientTest.java PROGRESS.md
git commit -m "Show the room id in a permanent header strip and the local player's own role/username in their side panel (or in the header for spectators), by finally reading the role that ROLE_ASSIGNED already sent and threading the logged-in username through to the game window"
```

עיצוב יותר יפה - קיצור מזהי matchmaking, ערכות נושא שונות ל-White/Black, תג "You" מעוגל (הסבב הזה, **טרם אומת ידנית - ר' "מה שנשאר לאמת" סעיף 15**):
```
git add src/main/java/kfchess/view/Img.java src/main/java/kfchess/view/GameSceneView.java src/main/java/kfchess/view/SidePanelView.java src/test/java/texttests/GameSceneViewTest.java PROGRESS.md
git commit -m "Restyle the room header and side panels: truncate long matchmaking UUIDs to 8 characters (Create/Join short codes stay untouched), give White and Black distinct color themes, and move the You indicator into a rounded badge below the panel title instead of squeezing it into the header line"
```

תיקון באג ניקוד - לכידה באוויר (הסבב הזה, **טרם אומת ידנית - ר' "מה שנשאר לאמת" סעיף 14**):
```
git add src/main/java/kfchess/engine/GameEngine.java src/main/java/kfchess/engine/MoveHistory.java src/test/java/texttests/GameEngineTest.java PROGRESS.md
git commit -m "Award the defending piece capture points when an attacker's move fails against a jumping defender, since the attacker actually vanishes from the board - previously neither side scored for this, even though it is a real capture from the defender's point of view"
```

## איך להריץ ולבדוק (IntelliJ)

1. Run על `kfchess.server.server.ServerMain` - אמורה להיכתב שורה
   `GameServer started on port 8887`.
2. Run על **`kfchess.LoginScreenMain`** - **זו הפעם היחידה שמריצים בתור
   לקוח/שחקנית** (אין יותר `main()` נפרד ב-`HomeScreenMain`/
   `NetworkGameWindowMain` - הוסרו בכוונה, ר' "ניקוי המיינים" למעלה).
   Register עם username+password חדשים (או Login אם כבר יש חשבון) →
   נפתח מסך הבית עם "Logged in as" ושני כפתורים: **"Play"** (matchmaking
   אקראי - היה "Skip") ו-**"Room..."** (פותח דיאלוג Create/Join/Cancel -
   שלב 6, החליף לגמרי את שדה ה-room+Connect הישן).
   ללחוץ **Room...** → **Create** - נכנס/ת כ-WHITE, וקוד החדר (6 תווים)
   מופיע בכותרת חלון המשחק.
3. **לשני שחקנים בו-זמנית**: בקונפיגורציית `LoginScreenMain` (Edit
   Configurations → Modify options → **Allow multiple instances**), ואז
   Run עליה **שוב** בלי לעצור את הריצה הראשונה - Register/Login עם
   username שני (או אותו אחד - עדיין לא קשור לתפקיד, ר' `ClientRole`),
   **Room...** → מקלידים את קוד החדר מהחלון הראשון → **Join** - נכנס/ת כ-BLACK.
4. בדיקת פרוטוקול גולמי: מקונסולת דפדפן (F12) עם `WebSocket` ישיר
   (`kfchess.server.client.ClientMain`, לקוח הקונסולה, הוסר - ר' "ניקוי המיינים").

## אימות ידני שבוצע בפועל

- **[סבב Restart] `mvn clean test` ירוק לגמרי** כולל כל הטסטים החדשים
  (`ClientCommandTest`, `GameSessionTest`, `NetworkClickHandlerTest`) -
  אושר ע"י רות.
- **[סבב Restart] הפיצ'ר אומת ידנית ועובד**: הגעה ל-game over, לחיצת
  Restart מצד אחד מציגה "Waiting for opponent..." בלי לאפס, ולחיצה גם
  מהצד השני מאפסת בפועל. אושר ע"י רות.
- **[שלב 4 Part B] ELO אומת ידנית ועובד** - רות שיחקה משחק אמיתי עד
  game over וראתה את הדירוג מתעדכן בפועל.
- `mvn clean test` - **ירוק לגמרי, 43/43** (אחרי הסרת 4 טסטים ב-
  `BoardParserTest` שציפו ל-`null` בעוד הקוד זורק חריגה בכוונה - אי-
  התאמה ישנה שתועדה כאן בעבר, טופלה סופית).
- שני חלונות `NetworkGameWindowMain` בו-זמנית (WHITE+BLACK): כל אחד
  יכול לבחור/להזיז רק את הכלים שלו (השרת דוחה קליק על כלי לא-שלו בשקט -
  זו התנהגות נכונה, לא באג), שני הלוחות מסונכרים בזמן אמת.
- סגירת חלון לקוח ופתיחת חדש: מצטרף מחדש לאותה `GameSession` שכבר
  קיימת (המצב חי על השרת, לא בלקוח) - **בכוונה**, לא באג. (הערה
  היסטורית - נכון לשלבים 1-4: באותו שלב עדיין לא היה טיפול בניתוק
  באמצע משחק פעיל בכלל. **מאז שלב 5 חלק 1 (ר' למטה) כן יש טיפול** -
  ניתוק WHITE/BLACK באמצע משחק פותח חלון חסד של 20 שניות, ואם לא
  מתחברים מחדש עם אותו username, היריב מוכרז כמנצח אוטומטית.)
- `HomeScreenMain` **אומת ידנית ע"י רות ועובד** - הזנת room, Connect,
  ומעבר לחלון המשחק (`NetworkGameWindowMain.launch`).
- **[שלב 5 חלק 1] `mvn clean test` ירוק לגמרי**, כולל כל הטסטים החדשים
  (`GameSessionTest` - 6 טסטים חדשים, `MessageDtoTest` - 2, `ClientSnapshotReconstructorTest` - 1) -
  אושר ע"י רות.
- **[שלב 5 חלק 1] תרחיש ה-timeout אומת ידנית ועובד**: ניתוק WHITE/BLACK
  באמצע משחק פעיל, המתנה 20 שניות בלי חיבור מחדש - היריב מוכרז כמנצח
  אוטומטית. אושר ע"י רות ("רץ מעולה עם ההמתנה של 20 שניות"). **עדיין
  לא אומתו ידנית** תרחיש ה-reconnect בתוך חלון החסד (חיבור מחדש עם אותו
  username מחזיר את אותו תפקיד) ותרחיש "לא נגנב" (חיבור שלישי לא תופס
  את הצבע השמור) - ר' "מה שנשאר לאמת".

## הצעד הבא

שלבים 1-4 **הושלמו ואומתו ידנית**, וכן פיצ'ר ה-Restart ההדדי (מעבר
לדרישות). ה-README נכתב מחדש (ר' סעיף למטה).

**שלב 5, חלק 1 (ניתוק/auto-resign) ממומש בקוד, `mvn test` ירוק, ותרחיש
ה-timeout אומת ידנית ועובד** (אושר ע"י רות - ר' "אימות ידני שבוצע
בפועל" למעלה). **נשארו לאמת רק שני תרחישי-קצה** (reconnect בתוך חלון
החסד, ו"לא נגנב") - ר' "מה שנשאר לאמת" סעיף 4 למעלה - לא חוסמים באופן
מהותי את המשך העבודה, אבל כדאי לוודא לפני commit סופי.

**שלב 5, חלק 2 (Matchmaking, כפתור "Skip"/"Play") ממומש בקוד, `mvn test`
ירוק, אומת ידנית - אושר ע"י רות.**

**שלב 6 (חדרים + לוגים) - שני החלקים הושלמו ואומתו במלואם - אושר ע"י
רות** ("הרצתי טסטים הרצתי חלוניות הכל עובד"). Create/Join/Cancel,
התיקון ל-Play/חדרים פרטיים, חסימת מהלכים בזמן המתנה, הבאנר הכחול,
והלוגים בצד שרת+לקוח - כולם ממומשים, נבדקו ב-`mvn test` (141/141) וגם
ידנית. **שלב 6 סגור.**

**התיקון המלא ל-"Play" לפי המפרט המקורי (ELO ±100 / timeout של דקה /
הודעת "לא נמצא") ממומש בקוד** - ר' הסעיף הייעודי למעלה. **ה-timeout
אומת ועובד בפועל - אושר ע"י רות** (חלון בודד, Play, אחרי 60 שניות
popup + סגירת החלון). **התאמת ה-ELO עצמה עדיין לא אומתה במפורש** (דורש
שני חשבונות עם דירוג מכוון רחוק/קרוב) - לא חוסם, `mvn test` עדיין לא
הורץ מאז השינוי הזה. **כל 6 השלבים ממומשים ואומתו ברמה המעשית** - מה
שנשאר הוא רק ליטוש/אימות סופי, לא פיצ'רים חסרים.

**מגבלה מודעת שנשארה פתוחה משלב 5 חלק 1** (לא נפתרה, לא הוחלט אם/מתי
לטפל בה): אם משחק מסתיים ע"י auto-resign (ניתוק), הצד שהתנתק **לא
מחובר יותר בכלל** - ולכן פיצ'ר ה-Restart ההדדי (ששני הצדדים צריכים
ללחוץ) לא ישים למעשה על המשחק הזה (רק הצד שנשאר יכול "להצביע", לעולם
לא יגיע לשניים). לא זיהיתי את זה כבאג - רק כתרחיש שלא טופל במפורש; אם
רות תרצה, אפשר בעתיד לתת לצד היחיד שנשאר לפתוח לבד משחק חדש (בלי
לחכות ל-restart ההדדי) כשה-gameOver נגרם ע"י ניתוק ולא ע"י לכידת מלך.

**כל 6 השלבים ממומשים בקוד.** מה שנשאר: לאמת (mvn test + ידנית) את
תיקון ה-Play האחרון (ר' למעלה) - ברגע שיאומת, כל הפרויקט (כולל המפרט
המדויק, לא רק "משהו שעובד") סגור לגמרי.

**מחוץ לשלבים, אם רות תרצה** (עלה בשיחה, לא הוחלט): הוספת plugin של
**JaCoCo** ל-`pom.xml` כדי למדוד אחוז כיסוי טסטים בפועל (המנחה ביקש 80%+).
כרגע אין דרך למדוד - אין JaCoCo מוגדר, ושכבת ה-UI (`view/`) ומחלקות
ה-main/שרת בלי טסטים ייעודיים, כך שהאחוז האמיתי כנראה נמוך מ-80%.

## סגנון עבודה מוסכם עם רות

- **לשאול ולהסביר לפני כל קובץ/מחלקה/פונקציה חדשה** - לא רק לפני
  שינויים ארכיטקטוניים גדולים. שורת הערה שמסבירה *למה* (לא רק *מה*)
  לפני כל פונקציה בקוד עצמו.
- **הסלמה מפורשת (סבב Restart)**: "תסביר לי כל פונקציה וכל שינוי שאתה
  עושה" - כלומר לא רק "לשאול לפני שינוי גדול", אלא להסביר **כל** שינוי,
  ברמת כל פונקציה, בצ'אט עצמו (לא רק בהערת קוד) - גם בשינויים קטנים/
  המשכיים. תקף לכל העבודה הנותרת בפרויקט הזה.
- **PROGRESS.md הוא בשביל ה-AI בלבד, לא בשביל רות** - רות לא קוראת
  אותו. כל הסבר מהותי (מה השתנה ולמה) חייב להיאמר בצ'אט במפורש, בנוסף
  לעדכון הקובץ - לא במקומו.
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
