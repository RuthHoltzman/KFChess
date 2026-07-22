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
4. חשבונות + ELO (SQLite) - 🟡 **Part A + Part B בוצעו** (ר' למטה),
   **טרם אומתו סופית** (`mvn test`/הרצה ידנית עם 2 חשבונות אמיתיים -
   ר' "מה שנשאר לאמת"). Part A: login/register מקומי. Part B: ה-username
   נשלח בפועל לשרת (query param על ה-URI) ו-ELO מתעדכן אוטומטית בסוף
   כל משחק (`EloCalculator`, K=32).
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
  - `kfchess.net.client` - `GameClient`, `IncomingMessageSummary`,
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

### `kfchess.net` + `kfchess.net.server` + `kfchess.net.client`
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

### `kfchess.net.client.NetworkClickHandler` (הוצא מ-NetworkGameWindowMain)
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

- **`kfchess.net.server.ServerMain`** - השרת, תהליך נפרד (תמיד היה ככה -
  זו המהות של client-server, לא "בלגן").
- **`kfchess.LoginScreenMain`** - **המיין היחיד ללקוח/למשחק בפועל**.
  Login/Register → `HomeScreenMain` (room) → `NetworkGameWindowMain`
  (המשחק) - שרשרת אחת, בלי לבחור בין כמה נקודות כניסה.
- **`kfchess.Main`** - **לא נגעתי בו בכלל** (המשחק קונסולה מקורי, לפני
  הרשת) - רות ציינה שהוא צריך להישאר בשביל טסטים/מטלה קודמת.
- **הוסרו לגמרי**: `kfchess.GameWindowMain` (משחק Swing מקומי, בלי שרת)
  ו-`kfchess.net.client.ClientMain` (לקוח קונסולה גולמי לבדיקת פרוטוקול) -
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
- **`kfchess.net.server.UsernameResolver`** (חדש, מקביל ל-`GameIdResolver`
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

## מה שנשאר לאמת (רות - עדיין לא נעשה, סבב Restart)

שוב: אין לי `javac`/`mvn` בסביבה שלי (אין root, אין גישת רשת להוריד
JDK/Maven) - בדקתי רק איזון סוגריים + חיפוש הפניות שבורות לכל קובץ
שנגעתי בו/נמחק, ואת נוסחת ה-ELO אימתתי גם בחישוב Python נפרד (לא רק
"נראה הגיוני") - אבל **זה לא תחליף ל-`mvn test` אמיתי**. צריך:

1. `mvn clean test` - לוודא שהכל מתקמפל וש**כל** הטסטים ירוקים, כולל
   סבב Part B (`EloCalculatorTest`×3, `UsernameResolverTest`×7,
   `GameIdResolverTest`, `SqliteAccountRepositoryTest`, `HomeScreenMainTest`)
   **וגם** סבב Restart החדש: `ClientCommandTest` (2 טסטים חדשים),
   `GameSessionTest` (4 טסטים חדשים), `NetworkClickHandlerTest` (2 טסטים
   חדשים + הבנאי המעודכן).
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
4. תזכורות מסבבים קודמים שעדיין רלוונטיות: קבצים לא-קשורים שכבר
   מופיעים כ-modified ב-`git status` (line-ending, לא תוכן - לא נגעתי
   בהם), ותיקיית `src/main/java/kfchess/.claude/` שלא יצרתי.

## פקודת commit מוצעת (לא הרצתי - רק `git status`/`git diff` לבדיקה)

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

## איך להריץ ולבדוק (IntelliJ)

1. Run על `kfchess.net.server.ServerMain` - אמורה להיכתב שורה
   `GameServer started on port 8887`.
2. Run על **`kfchess.LoginScreenMain`** - **זו הפעם היחידה שמריצים בתור
   לקוח/שחקנית** (אין יותר `main()` נפרד ב-`HomeScreenMain`/
   `NetworkGameWindowMain` - הוסרו בכוונה, ר' "ניקוי המיינים" למעלה).
   Register עם username+password חדשים (או Login אם כבר יש חשבון) →
   נפתח מסך הבית עם "Logged in as" → מזינים room (או משאירים `default`)
   ולוחצים Connect - נכנס כ-WHITE.
3. **לשני שחקנים בו-זמנית**: בקונפיגורציית `LoginScreenMain` (Edit
   Configurations → Modify options → **Allow multiple instances**), ואז
   Run עליה **שוב** בלי לעצור את הריצה הראשונה - Register/Login עם
   username שני (או אותו אחד - עדיין לא קשור לתפקיד, ר' `ClientRole`),
   ואז Connect לאותו room (`default` אם לא שינו) - נכנס כ-BLACK.
4. בדיקת פרוטוקול גולמי: מקונסולת דפדפן (F12) עם `WebSocket` ישיר
   (`kfchess.net.client.ClientMain`, לקוח הקונסולה, הוסר - ר' "ניקוי המיינים").

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

**שלב 4 שלם** (Part A + Part B) - ה-ELO **כבר אומת ידנית ע"י רות ועובד**.
**פיצ'ר Restart שלם בקוד** (ר' סעיף למעלה) - **טרם אומת ידנית** (ר' "מה
שנשאר לאמת", סעיף 3 - זה הצעד המיידי הבא: להריץ בפועל ולוודא ששני
הצדדים חייבים ללחוץ, וש-"Waiting for opponent..." מוצג נכון). אחרי
האימות הזה, השלב הבא הוא **שלב 5**: Matchmaking (כפתור "Play" שמוצא
יריבה אוטומטית, במקום להקליד שם room ידנית) + טיפול בניתוק (auto-resign) -
כרגע אם צד מתנתק באמצע משחק, שום דבר לא קורה (המשחק פשוט קופא, אף אחד
לא מפסיד, ה-ELO לא מתעדכן). אחריו שלב 6: חדרים אמיתיים (Create/Join/Cancel)
+ לוגים.

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
