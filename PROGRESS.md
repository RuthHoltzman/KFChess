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
5. Matchmaking ("Play"/Skip) + ניתוק/auto-resign - 🟡 **auto-resign הושלם
   ואומת לגמרי, committed.** ה-matchmaking עצמו (כפתור "Skip", עדיין לא
   שונה שם ל-"Play") **לא לגמרי לפי המפרט המקורי** - ר' "הערה חשובה -
   פער ידוע משלב 5" למטה (נמצא רק אחרי שרות סיפקה את ה-PDF המקורי;
   הוחלט במפורש עם רות **לדחות את התיקון** ולהתקדם לשלב 6 קודם).
6. חדרים (Create/Join/Cancel) + לוגים - 🟡 **חלק 1 (Create/Join/Cancel)
   ממומש בקוד הסבב הזה, טרם אומת.** חלק 2 (לוגים בצד שרת+לקוח) - עדיין
   לא התחיל, סבב נפרד מתוכנן.

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

## מה שנשאר לאמת (רות - עדיין לא נעשה)

שוב: אין לי `javac`/`mvn` בסביבה שלי (אין root, אין גישת רשת להוריד
JDK/Maven) - בדקתי רק איזון סוגריים + חיפוש הפניות שבורות לכל קובץ
שנגעתי בו/נמחק, ואת נוסחת ה-ELO אימתתי גם בחישוב Python נפרד (לא רק
"נראה הגיוני") - אבל **זה לא תחליף ל-`mvn test` אמיתי**. צריך:

1. ~~`mvn clean test`~~ - **בוצע לשלב 5 חלק 1, ירוק לגמרי - אושר ע"י רות.**
   **טרם בוצע לשלב 5 חלק 2 (matchmaking, הסבב הזה)** - כולל הטסטים
   החדשים: `GameSessionTest` (5 חדשים), `MatchmakingResolverTest` (חדש
   לגמרי), `HomeScreenMainTest` (3 חדשים).
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
6. **הרצה ידנית של חדרים (Create/Join/Cancel, חדש, טרם נבדק בפועל כלל -
   הסבב הזה)**:
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
7. תזכורות מסבבים קודמים שעדיין רלוונטיות: קבצים לא-קשורים שכבר
   מופיעים כ-modified ב-`git status` (line-ending, לא תוכן - לא נגעתי
   בהם), ותיקיית `src/main/java/kfchess/.claude/` וקובץ `kfchess.db`
   שלא יצרתי (untracked, לא ב-git add המוצע).

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

**שלב 5, חלק 2 (Matchmaking, כפתור "Skip"/"Play") ממומש בקוד** - ר'
הסעיף הייעודי למעלה. **טרם אומת** (לא `mvn test` ולא ידנית) - ר' "מה
שנשאר לאמת" סעיף 5 למעלה. **בנוסף - סטייה ידועה מהמפרט המקורי (התגלתה
רק אחרי שרות סיפקה את ה-PDF)**: אין סינון ELO ±100, אין timeout של דקה,
אין הודעת "לא נמצא" - ר' "הערה חשובה - קובץ ההוראות המקורי" למעלה.
**רות בחרה במפורש לדחות את התיקון הזה** ולהתקדם לשלב 6 קודם - עדיין
פתוח, צריך לחזור אליו.

**שלב 6, חלק 1 (חדרים - Create/Join/Cancel) ממומש בקוד בסבב הזה** - ר'
הסעיף הייעודי למעלה. **טרם אומת** (לא `mvn test` ולא ידנית) - ר' "מה
שנשאר לאמת" סעיף 6 למעלה. אחרי שיאומת ויעשה commit - **שלב 6, חלק 2
(לוגים בצד שרת+לקוח, לקובץ טקסט - בחירת רות)** הוא הצעד הבא, וגם
(בנפרד) לחזור לתיקון ה-matchmaking שנדחה משלב 5.

**מגבלה מודעת שנשארה פתוחה משלב 5 חלק 1** (לא נפתרה, לא הוחלט אם/מתי
לטפל בה): אם משחק מסתיים ע"י auto-resign (ניתוק), הצד שהתנתק **לא
מחובר יותר בכלל** - ולכן פיצ'ר ה-Restart ההדדי (ששני הצדדים צריכים
ללחוץ) לא ישים למעשה על המשחק הזה (רק הצד שנשאר יכול "להצביע", לעולם
לא יגיע לשניים). לא זיהיתי את זה כבאג - רק כתרחיש שלא טופל במפורש; אם
רות תרצה, אפשר בעתיד לתת לצד היחיד שנשאר לפתוח לבד משחק חדש (בלי
לחכות ל-restart ההדדי) כשה-gameOver נגרם ע"י ניתוק ולא ע"י לכידת מלך.

אחרי שלב 5: **שלב 6** - חדרים אמיתיים (Create/Join/Cancel כמסכים נפרדים)
+ לוגים.

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
