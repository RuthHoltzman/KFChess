# KFChess Server - Progress & Handoff

מסמך זה נועד לתת לשיחת Claude חדשה (או לרות) תמונת מצב מלאה בלי צורך
לגלול היסטוריית צ'אט ארוכה. בתחילת שיחה חדשה: תני לינק/תגידי "תקראי
את PROGRESS.md בתיקיית הפרויקט ותמשיכי משם".

## המטלה

הוספת צד שרת לפרויקט KFChess (Java, Maven, Java 17) - שחמט בזמן-אמת
("קונג-פו צ'אס") שקיים היום כאפליקציית Swing מקומית (`GameWindowMain`).
לפי מצגת ההוראות (`CTD 26 (Server).pptx.pdf`), חלוקה ל-6 שלבים:

1. **Bus (pub/sub)** - ✅ הושלם, committed.
2. **שרת WebSocket חד-תהליכי בסיסי** - ✅ **הקוד נכתב במלואו** (ר' למטה).
   **עדיין לא הורץ בפועל ולא committed** - רות צריכה להריץ `mvn test`
   ואז לבדוק חיבור אמיתי (ר' "איך לבדוק שזה עובד" למטה).
3. Home screen v1 - login בשל (shell), 2 שחקנים בלבד - ⬜ לא התחיל.
4. חשבונות + ELO (SQLite) - ⬜ לא התחיל.
5. Matchmaking (Play) + ניתוק/auto-resign - ⬜ לא התחיל.
6. חדרים (Create/Join/Cancel) + לוגים - ⬜ לא התחיל.

## החלטות ארכיטקטורה שנסגרו (Phase 2) - כולן ממומשות בפועל

- ספריית WebSocket: **Java-WebSocket 1.6.0** (org.java-websocket) - **נוסף** ל-pom.xml.
- JSON: **Gson 2.14.0** - **נוסף** ל-pom.xml.
- פרוטוקול הודעות: אובייקט JSON עם שדה `"type"`. קליק/קפיצה נשלחים כ-
  `{"type":"CLICK","row":..,"col":..}` / `{"type":"JUMP",...}` (מיקום על
  הלוח, לא פיקסלים).
- מבנה השרת: `Map<gameId, GameSession>` ב-`GameServer`, נוצר lazily.
  **gameId נלקח מנתיב החיבור עצמו** (`ws://host:port/room1` → "room1",
  חיבור לשורש → "default") - כדי לתמוך בכמה משחקים במקביל גם בלי UI
  לחדרים (שלב 6 עדיין לא קיים). מומש ב-`GameIdResolver` (מחלקה נפרדת,
  טהורה וניתנת לבדיקה, לא מתודה פרטית בתוך GameServer).
- **מודל תפקידים**: `ClientRole{WHITE,BLACK,SPECTATOR}` - רחב יותר מ-
  `PieceColor` כי לצופה אין צבע כלל. הראשון שמתחבר ל-GameSession=WHITE,
  השני=BLACK, כל השאר=SPECTATOR.
- **מודל thread-safety ("שרת חד-תהליכי")**: כל שינוי במצב המשחק (GameEngine)
  קורה אך ורק מ-thread אחד - thread הטיק. פקודות שמגיעות מ-threads הרשת
  (`onMessage`) לא נוגעות ב-engine בכלל, רק נכנסות לתור
  (`GameSession.enqueueCommand`, `ConcurrentLinkedQueue`); `tick()` הוא
  היחיד שמרוקן את התור ומפעיל את הפקודות, ואז מקדם את שעון המשחק.
- **חלוקת אחריות GameSession מול GameServer**: GameSession לא יודע כלום
  על Gson/רשת - הוא רק מחזיק מצב משחק ובונה DTO (`SnapshotMessage`).
  GameServer אחראי בלעדית על סריאליזציה + שליחה בפועל + ניתוב gameId.
- **snapshot מאוחד (לא "מקומי" מול "רשת" נפרדים)**: `SnapshotMessage`
  כולל גם `motions`/`jumps`/`captureEffects` - בדיוק אותו מידע שה-UI
  המקומי (Swing, SnapshotFactory) כבר מצייר כאנימציה, נלקח ישירות מ-
  `engine.activeMotions()/activeJumps()/recentCaptureEffects()` (כבר
  היו public על GameEngine, שום לוגיקה כפולה לא נכתבה). הלקוח מחשב
  התקדמות אנימציה (0..1) מתוך `now` (מגיע באותה הודעה) מול
  startTime/arrivalTime של כל פריט - אין צורך בסנכרון שעונים בין
  שרת ללקוח.
- **מינימום DTOs, לא שכבה נפרדת "בשביל העיקרון"**: `Position`/`Motion`/
  `CaptureEffect` (המחלקות האמיתיות של הפרויקט) נשלחות ישירות ל-Gson
  בלי עטיפה - הן כבר בדיוק בצורה הרצויה, ו-Gson מסריאלז כל אובייקט
  Java לבד (לא צריך getters/DTO). **רק** `PieceDto`/`JumpDto` נשארו,
  כי `Piece`/`JumpVisual` **בכוונה** לא יודעים את מיקומם על הלוח
  (Board הוא מקור האמת היחיד למיקום) - אז צריך משהו שמצמיד להם מיקום
  מבחוץ; שני אלה עצמם רק עוטפים את האובייקט האמיתי + Position, בלי
  להעתיק אף שדה ידנית. (הוחלט לצמצם מ-`PositionDto`/`MotionDto`/
  `CaptureEffectDto` שנכתבו קודם ונמחקו - אין עדיין אף לקוח שתלוי
  בצורת ה-JSON, אז אין עלות "תאימות לאחור" לצמצום הזה.)

## מה שכבר קיים ומוכן לשרת (מלפני השיחה הזו, committed)

### Bus (`kfchess.bus`)
`EventBus` גנרי (subscribe/publish לפי סוג), 4 סוגי אירועים:
`ScoreUpdatedEvent`, `MoveLoggedEvent`, `SoundEvent`, `GameLifecycleEvent`.

### מבנה `kfchess.engine`
`GameEngine` (חוקי המשחק), `PieceTimers`, `MoveHistory`, `NetworkActions`
(מנתב קליק/קפיצה לפי צבע שחקן - זה מה ש-GameSession משתמש בו), `snapshot/`
(לא רלוונטי לשרת - רינדור Swing מקומי בלבד).

## מה שנוצר בשיחה הזו - חבילת `kfchess.net` (לא committed עדיין!)

קבצי production (12, אחרי צמצום - ר' "מינימום DTOs" למעלה):
```
kfchess/net/
  ClientCommandType.java   - enum CLICK/JUMP
  ClientCommand.java       - DTO נכנס (type/row/col) + isValid()
  PieceDto.java            - מצמיד Piece+Position (Piece לא יודע מיקום בעצמו)
  JumpDto.java             - מצמיד JumpVisual+Position (אותה סיבה)
  ClientRole.java          - WHITE/BLACK/SPECTATOR + toPieceColor()
  RoleAssignedMessage.java - נשלח פעם אחת ב-onOpen
  ErrorMessage.java        - JSON פגום/פקודה לא תקינה
  SnapshotMessage.java     - מצב הלוח המלא (כולל אנימציות), נשלח בכל טיק;
                             Position/Motion/CaptureEffect משודרים ישירות בלי DTO
  GameIdResolver.java      - resourceDescriptor -> gameId (טהור, נבדק ביחידה)
  GameSession.java         - "המוח" של משחק בודד: תור פקודות + tick() + snapshotFor()
  GameServer.java          - extends WebSocketServer, Map<gameId,GameSession>,
                             onOpen/onClose/onMessage/onError/onStart, לולאת טיק
                             על ScheduledExecutorService נפרד
  ServerMain.java          - main(), פורט ברירת מחדל 8887 (args[0] לשינוי)
```

קבצי טסט (`src/test/java/texttests/`, לפי הבקשה של רות - טסט לכל
פונקציה/לוגיקה משמעותית):
```
ClientCommandTest, PieceDtoTest, JumpDtoTest, MessageDtoTest,
GameIdResolverTest, GameSessionTest (9 מקרים, כולל: הקצאת תפקידים, הסרת
חיבור, ניתוב פקודות לפי צבע, ביצוע מהלך בפועל (IN_TRANSIT + motions
תואם), התעלמות מפקודת צופה, חוסן מול חיבור לא-מזוהה, מצב פתיחה),
FakeWebSocket (עזר טסטים בלבד - stub ל-org.java_websocket.WebSocket,
לא נקרא בפועל ע"י GameSession אלא רק משמש כמפתח-זהות).
```

גם `.gitignore` עודכן (נוסף `.idea/` ו-`*.iml`) ו-`pom.xml` (Java-WebSocket + Gson).

**שום דבר מכל זה עדיין לא בוצע לו commit** - כל commit עד עכשיו רק
הוצע בצ'אט, רות מריצה בעצמה.

## בעיה קיימת שהתגלתה ולא טופלה (לא קשורה לעבודת השרת)

`BoardParserTest` (4 מתוך 6 הטסטים שלו) נכשל ב-`mvn test`: הטסטים
מצפים ש-`BoardParser.readBoard()` יחזיר `null` על קלט פגום, אבל הקוד
בפועל **זורק** `IllegalArgumentException` (ובכוונה - יש הערת קוד מפורשת
על כך ב-BoardParser.java). כנראה טסט ישן שלא עודכן אחרי שינוי התנהגות.
**הוחלט עם רות להשאיר את זה בצד בינתיים** ולא לתקן כחלק מעבודת השרת.

## איך לבדוק שהשרת עובד (אחרי שרות מריצה `mvn test`/`mvn compile` בהצלחה)

1. IntelliJ: לפתוח את `ServerMain.java` → Run על ה-`main`. אמורה להיכתב
   שורה `GameServer started on port 8887`.
2. לבדוק חיבור מקונסולת דפדפן (F12):
   ```js
   let ws = new WebSocket("ws://localhost:8887/default");
   ws.onmessage = e => console.log(e.data);
   ws.send('{"type":"CLICK","row":6,"col":4}');
   ```
   אמורה לקפוץ מיד הודעת `ROLE_ASSIGNED`, ואז זרם `SNAPSHOT` כ-30
   פעמים בשנייה; אחרי ה-CLICK אמור להופיע `"selected":{"row":6,"col":4}`
   באחד ה-snapshots הבאים.

## אימות ידני שכבר בוצע (השרת עובד!)

רות הריצה בפועל: `mvn test` (ירוק, חוץ מ-BoardParserTest הידוע),
הרצת `ServerMain` (SLF4J warning - לא-מזיק, אין provider מוגדר -
תקין), וחיבור אמיתי מקונסולת דפדפן ל-`ws://localhost:8887/default` -
התקבלו `ROLE_ASSIGNED` וזרם `SNAPSHOT` תקין עם 32 כלים בפריסת הפתיחה
הנכונה. **שלב 2 מאומת end-to-end.**

## מה שנוצר בשיחה הזו - התחלת "לקוח" (GameClient מינימלי, בלי GUI)

לפני חיבור מלא ל-`GameWindowMain` (ר' "הצעד הבא"), נבנה קודם לקוח
מינימלי מבוסס-קונסולה כדי לוודא שהתקשורת בכיוון ההפוך (לקוח → שרת)
עובדת, בלי לגעת עדיין ב-GUI:

```
kfchess/net/
  GameClient.java            - extends org.java_websocket.client.WebSocketClient.
                               onOpen/onMessage/onClose/onError מדפיסים למסוף;
                               sendClick(row,col)/sendJump(row,col) בונים
                               ClientCommand וממירים ל-JSON.
  IncomingMessageSummary.java - הופך הודעת JSON נכנסת לשורה קריאה אחת לפי
                               שדה "type" (ROLE_ASSIGNED/SNAPSHOT/ERROR) -
                               כדי לא להציף מסוף כמו בבדיקת הדפדפן; לוגיקה
                               טהורה, נבדקת בלי חיבור רשת (כמו GameIdResolver).
  ClientMain.java             - main(): connectBlocking() ואז לולאת קונסולה
                               ("click ROW COL"/"jump ROW COL"/"quit") - מאפשר
                               לבדוק תקשורת מלאה מטרמינל Java, בלי דפדפן.
```

שינוי בקובץ קיים: **`ClientCommand.java`** קיבל בנאי ציבורי חדש
(`ClientCommand(ClientCommandType, int, int)`) - עד עכשיו רק השרת בנה
אותו (דרך Gson.fromJson); עכשיו גם הלקוח יכול לבנות פקודה יוצאת בעצמו,
באותו DTO (בלי לשכפל מבנה).

טסט חדש: `IncomingMessageSummaryTest` (4 מקרים - שלושת סוגי ההודעות +
type לא מזוהה).

**עודכן אחרי בדיקה ידנית ראשונה** (רות הריצה, גילתה שהקונסולה מוצפת
ב-SNAPSHOT כ-30/שנייה ואי אפשר להקליד): `GameClient.onMessage` כבר לא
מדפיס SNAPSHOT אוטומטית - רק שומר אותו בשקט (`latestMessage`); מדפיס
מיד רק הודעות נדירות (ROLE_ASSIGNED/ERROR). נוספה פקודת `status` ל-
`ClientMain` שמדפיסה את ה-snapshot האחרון לפי דרישה. `IncomingMessageSummary`
קיבל גם `selected` בתקציר וגם `isSnapshot()`/`messageType()` חדשים.
**חשוב:** זה משנה רק מה ש-`ClientMain` (כלי בדיקה זמני) מדפיס למסוף -
`GameClient` ימשיך לקבל ולעבד כל הודעה תמיד; כשיתחבר ל-GUI, "print"
יוחלף ב"עדכן את הציור", בכל ההודעות, באותו קצב מלא.

**עדיין לא נבדק בפועל אחרי העדכון הזה** (רות צריכה: `mvn test`, ואז
`ClientMain` מול `ServerMain` רץ - "click 6 4" ואז "status").

## הצעד הבא (איפה להמשיך)

אחרי ש-GameClient המינימלי מאומת: **לחבר בפועל את `GameWindowMain`**
כך שיוכל לרוץ במצב רשת. הבעיה המרכזית שצריך לפתור: GameWindowMain
מצייר לפי `GameSnapshot` שנבנה ע"י `SnapshotFactory` מתוך אובייקטי
דומיין אמיתיים (Piece/Motion/JumpVisual) - GameClient מקבל רק JSON.
ההחלטה שהתקבלה: **בצד הלקוח בלבד** לשחזר אובייקטים זמניים מה-JSON
ולהזין אותם ל-SnapshotFactory הקיים בלי לשנות אותו - כך קוד הציור
נשאר משותף למשחק מקומי ולמשחק-רשת (ה-View **תמיד** נשאר בלקוח, השרת
לא יודע עליו כלום - ר' דיון בצ'אט על ההפרדה client/server).

## סגנון עבודה מוסכם עם רות

- לשאול לפני החלטות ארכיטקטורה משמעותיות (AskUserQuestion / בצ'אט),
  **ולהסביר לפני כל שינוי בקבצים - לא רק להריץ Write/Edit בלי להודיע**.
- לוודא שהגישה המוצעת היא "הדרך המומלצת" (למשל: השוואת Java-WebSocket
  מול Spring Boot, בדיקת דפוס queue+tick-thread מול מקורות ברשת) לפני
  שממשיכים.
- לערוך את הקבצים ישירות (לא רק להציג קוד בצ'אט להעתקה).
- **לכתוב טסט לכל פונקציה/לוגיקה משמעותית** (יש לרות תיקייה ייעודית -
  `src/test/java/texttests/`) - לא רק ל-production code אלא גם ל-DTOs.
- **אחרי כל שינוי: להציע פקודת commit מדויקת (git add + git commit -m)
  אבל לא להריץ אותה בעצמי** - רות מריצה. חשוב: לא לגעת בקבצים אחרים
  שכבר יש לה שינויים לא-committed בהם (יש כמה כאלה מעבודה שלה עצמה).
- אחרי כל שינוי מבני: לבדוק איזון סוגריים + חיפוש הפניות ישנות שנשברו,
  לפני שמבקשים ממנה להריץ `mvn compile`/`mvn test` (אין JDK/Maven
  מלאים בסביבת הכלים - יש רק JRE 11, אין javac, אין הרשאות התקנה).
- רות שמה דגש חזק על קוד מסודר/לא-ארוך-מדי - להעדיף מחלקות קטנות
  וממוקדות (single responsibility). דוגמה מהשיחה הזו: `GameIdResolver`
  הוצא כמחלקה נפרדת מ-GameServer בדיוק כדי שיהיה ניתן לבדוק אותו ביחידה.
