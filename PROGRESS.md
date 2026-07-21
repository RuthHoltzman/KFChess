# KFChess Server - Progress & Handoff

מסמך זה נועד לתת לשיחת Claude חדשה (או לרות) תמונת מצב מלאה בלי צורך
לגלול היסטוריית צ'אט ארוכה. בתחילת שיחה חדשה: תני לינק/תגידי "תקראי
את PROGRESS.md בתיקיית הפרויקט ותמשיכי משם".

## המטלה

הוספת צד שרת לפרויקט KFChess (Java, Maven, Java 17) - שחמט בזמן-אמת
("קונג-פו צ'אס") שקיים היום כאפליקציית Swing מקומית (`GameWindowMain`).
לפי מצגת ההוראות (`CTD 26 (Server).pptx.pdf`), חלוקה ל-6 שלבים:

1. **Bus (pub/sub)** - ✅ הושלם, committed.
2. **שרת WebSocket חד-תהליכי בסיסי** - 🔶 בעבודה. הוכן תשתית (ר' למטה),
   **קוד השרת עצמו עוד לא נכתב**.
3. Home screen v1 - login בשל (shell), 2 שחקנים בלבד - ⬜ לא התחיל.
4. חשבונות + ELO (SQLite) - ⬜ לא התחיל.
5. Matchmaking (Play) + ניתוק/auto-resign - ⬜ לא התחיל.
6. חדרים (Create/Join/Cancel) + לוגים - ⬜ לא התחיל.

## החלטות ארכיטקטורה שכבר נסגרו (Phase 2)

- ספריית WebSocket: **Java-WebSocket** (org.java-websocket) - עדיין
  **לא נוסף** ל-pom.xml.
- JSON: **Gson** - עדיין **לא נוסף** ל-pom.xml.
- פרוטוקול הודעות: אובייקט JSON עם שדה `"type"` (לא מחרוזת פקודה קצרה).
  קליק/קפיצה נשלחים כ-`{"type":"CLICK","row":..,"col":..}` (מיקום על
  הלוח, לא פיקסלים - הלקוח כבר עושה pixel→Position בעצמו לפני השליחה,
  ר' `BoardMapper`).
- מבנה השרת: `Map<gameId, GameSession>` - תומך בכמה משחקים במקביל
  מההתחלה (גם לפני שיש UI לחדרים). כל `GameSession` מחזיק `GameEngine`
  משלו + `EventBus` משלו + את חיבורי ה-WebSocket שמשתתפים בו (לבן/שחור/
  צופים - הראשון שמתחבר=לבן, השני=שחור, השאר=צופים).
- **עדיין לא נכתבו הקבצים בפועל**: `kfchess.net` package (או שם דומה)
  עם `GameServer`, `GameSession`, `ClientCommand`, `SnapshotMessage`
  וכו'. השיחה עברה לניקוי ארכיטקטוני של `GameEngine` לפני שהגענו לכתוב
  את קוד השרת עצמו - זו הנקודה הבאה להמשיך ממנה.

## מה שכבר קיים ומוכן לשרת (בוצע ו-committed)

### Bus (`kfchess.bus`)
`EventBus` גנרי (subscribe/publish לפי סוג), 4 סוגי אירועים:
`ScoreUpdatedEvent`, `MoveLoggedEvent`, `SoundEvent`, `GameLifecycleEvent`.
`GameEngine` מקבל `EventBus` בקונסטרוקטור ומפרסם בדיוק בנקודות שהוא
כבר מעדכן ניקוד/יומן/סיום משחק.

### מבנה `kfchess.engine` (אחרי refactor)
```
kfchess/engine/
  GameEngine.java     (398 שורות) - רק חוקי המשחק
  PieceTimers.java    - תזמוני קפיצה/מנוחה לכלי (package-private methods
                        על GameEngine: isAvailableToAct, beginJump,
                        tryMove, advanceGameState - חשופים ל-NetworkActions)
  MoveHistory.java    - ניקוד + יומן מהלכים
  NetworkActions.java - **קיים ומוכן** - מנתב קליק/קפיצה לפי צבע שחקן,
                        עם בחירה (selection) נפרדת לכל צבע (בניגוד ל-
                        GameEngine.handleClick(Position) הרגיל, שמשותף
                        לכל השחקנים - מתאים למשחק מקומי, לא לרשת).
                        השרת יחזיק NetworkActions אחד לכל GameSession
                        ויקרא ל-networkActions.handleClick(color, pos)
                        במקום ל-engine.handleClick(pos).
  snapshot/           - הכל שקשור אך ורק לרינדור UI מקומי (Swing), לא
                        רלוונטי לשרת בכלל: GameSnapshot, SnapshotFactory,
                        PieceVisualState(Tracker), JumpVisual,
                        CaptureEffect(Snapshot/Tracker), PieceSnapshot.
```

### מודל המשחק הרלוונטי לפרוטוקול
- `Board.pieceAt(Position)` - לבניית snapshot (לולאה כפולה row/col, אין
  מיפוי position→piece ישיר ב-Board).
- `Piece.color()/kind()/state()` - `PieceColor{WHITE,BLACK}`,
  `PieceKind{KING,QUEEN,ROOK,BISHOP,KNIGHT,PAWN}`,
  `PieceState{IDLE,IN_TRANSIT,JUMPING}`.
- `GameEngine.scores()` / `.moveLog()` / `.isGameOver()` / `.winner()`.

## הצעד הבא (איפה להמשיך)

לכתוב את קוד השרת בפועל:
1. להוסיף ל-`pom.xml`: Java-WebSocket + Gson.
2. חבילה חדשה (למשל `kfchess.net`): DTOs (`ClientCommand`,
   `SnapshotMessage`, `PieceDto`, `RoleAssignedMessage`, `ErrorMessage`),
   `GameSession` (עוטף Board+Game+GameEngine+EventBus+NetworkActions
   +רשימת חיבורים), `GameServer extends WebSocketServer`
   (`Map<String,GameSession>`, `onOpen/onClose/onMessage`, tick loop
   תקופתי שקורא ל-`engine.handleWait` ומשדר snapshot).
3. אחרי שהשרת עובד - לקוח: `GameClient` (WebSocket client) + לחבר את
   `GameWindowMain` אליו (מצב רשת, לא רק מקומי).

## סגנון עבודה מוסכם עם רות

- לשאול לפני החלטות ארכיטקטורה משמעותיות (AskUserQuestion / בצ'אט).
- לערוך את הקבצים ישירות (לא רק להציג קוד בצ'אט להעתקה - זה היה בשלב
  מוקדם יותר וגרם לבאגים בהעתקה ידנית).
- אחרי כל שינוי מבני: לבדוק איזון סוגריים + חיפוש הפניות ישנות שנשברו,
  לפני שמבקשים ממנה להריץ `mvn compile` (אין JDK 17 בסביבת הכלים).
- רות שמה דגש חזק על קוד מסודר/לא-ארוך-מדי - להעדיף מחלקות קטנות
  וממוקדות (single responsibility) על פני הוספת עוד ועוד ל-GameEngine.
