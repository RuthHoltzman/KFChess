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
3. Home screen v1 - login בשל (shell), 2 שחקנים בלבד - ⬜ **הצעד הבא** (ר' למטה).
4. חשבונות + ELO (SQLite) - ⬜ לא התחיל.
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

## איך להריץ ולבדוק (IntelliJ)

1. Run על `kfchess.net.server.ServerMain` - אמורה להיכתב שורה
   `GameServer started on port 8887`.
2. Run על `kfchess.NetworkGameWindowMain` - נכנס כ-WHITE.
3. **לשני שחקנים בו-זמנית**: בקונפיגורציית `NetworkGameWindowMain`
   (Edit Configurations → Modify options → **Allow multiple instances**),
   ואז Run עליה **שוב** בלי לעצור את הריצה הראשונה - נכנס כ-BLACK, לאותו
   URI ברירת מחדל (`ws://localhost:8887/default`), כדי להיכנס לאותה
   `GameSession`.
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

## הצעד הבא

**שלב 3**: Home screen v1 - כרגע `NetworkGameWindowMain` מתחבר ישר
ל-URI קבוע (`ws://localhost:8887/default`, או מ-`args[0]`) בלי שום
מסך פתיחה. הצעד הבא הוא מסך login/home בסיסי (shell - בלי חשבונות
אמיתיים עדיין, זה שלב 4) שמאפשר לבחור/להזין room ולהתחבר משם, בשביל
2 שחקנים בלבד (לא matchmaking - זה שלב 5).

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
