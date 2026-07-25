# KFChess — Real-Time ("Kung-Fu") Chess

A real-time chess variant in Java: there are **no turns**. Both players may move
at any moment, pieces take **time to travel** between squares, and the board only
changes when a piece actually *arrives*. The game ends when a king is captured.

The project runs as a **client–server** application over WebSocket, with local
user accounts and automatic ELO rating updates.

---

## Requirements

- **Java 17** (JDK, not just JRE)
- **Maven 3.6+**

No manual dependency downloads are needed — Maven fetches everything
(`Java-WebSocket`, `Gson`, `sqlite-jdbc`, `jBCrypt`, JUnit 5).

---

## Build & Test

```bash
mvn clean test      # compile everything and run the full test suite
mvn clean package   # build the jar
```

Running the application itself is done from the IDE (see below) — the classes are
launched directly rather than through a packaged executable jar.

---

## How to Run

The game needs **one server process** and **one client process per player**.

### 1. Start the server

Run the class `kfchess.net.server.ServerMain` — in IntelliJ, open the file and
click the green ▶ next to `main`, or right-click the file → **Run 'ServerMain.main()'**.

You should see:

```
GameServer started on port 8887
```

Leave it running. It also creates/uses the accounts database file `kfchess.db`
in the working directory (created automatically on first use).

### 2. Start a client (once per player)

Run the class **`kfchess.LoginScreenMain`** — this is the **only** entry point
for playing the game.

The flow is:

1. **Login / Register** — enter a username and password. `Register` creates a new
   account (starting ELO 1200); `Login` signs into an existing one.
2. **Home screen** — shows `Logged in as <username>`. Enter a *room* name
   (or leave the default `default`) and press **Connect**.
3. **Game window** — the first client to join a room is **White**, the second is
   **Black**, anyone after that is a **spectator** (can watch, cannot move).

### 3. Playing with two players on one machine

Start the server once, then start `LoginScreenMain` **twice**.

In IntelliJ you must allow this explicitly:
`Edit Configurations…` → `Modify options` → **Allow multiple instances**.

Log in with **two different accounts** and connect both to the **same room name**.

> Using two *different* accounts matters if you want to see ELO change — a game
> where both sides are the same account is deliberately skipped for rating.

---

## Controls

| Action | Input |
|---|---|
| Select a piece | Left-click it |
| Move the selected piece | Left-click the destination square |
| Jump / special action | Right-click |
| Restart after game over | Click **Restart** — **both** players must click it |

You can only select and move **your own** color. The server silently ignores
clicks on pieces that aren't yours — that is intended behaviour, not a bug.

When the game ends, an overlay shows the winner and a **Restart** button.
Restart requires **mutual agreement**: after one side clicks it, both screens
show *"Waiting for opponent…"*, and the board only resets once the second
player clicks too.

---

## Project Structure

```
src/main/java/kfchess/
├── model/              Board, Piece, Position, colors/kinds/states
├── rules/              RuleEngine + PieceRules (movement legality per piece)
├── realtime/           RaelTime (game clock) + Motion (a piece in transit)
├── engine/             GameEngine, MoveHistory, NetworkActions
│   └── snapshot/       SnapshotFactory + the immutable GameSnapshot view model
├── bus/                EventBus (pub/sub) + the 4 game event types
├── io/                 BoardParser / BoardPrinter (text board format)
├── view/               Swing rendering: BoardView, GameSceneView, Img, animations
│   └── layout/         BoardLayoutCalculator (screen geometry)
├── input/              BoardMapper, Controller (pixel ↔ board coordinates)
├── account/            Accounts, password hashing, SQLite repository, EloCalculator
├── net/                Shared protocol DTOs (ClientCommand, SnapshotMessage, …)
│   ├── server/         GameServer, GameSession, ServerMain, resolvers
│   └── client/         GameClient, snapshot reconstruction, NetworkClickHandler
├── LoginScreenMain     ← entry point: log in / register
├── HomeScreenMain      ← room selection
├── NetworkGameWindowMain  ← the game window itself
└── Main                ← original console version (kept for earlier assignment)
```

### Entry points

| Class | Purpose |
|---|---|
| `kfchess.net.server.ServerMain` | The WebSocket server (run once) |
| `kfchess.LoginScreenMain` | The player client (run once per player) |
| `kfchess.Main` | Original text/console version of the game |

---

## How It Works

- **Protocol** — JSON over WebSocket. Clients send
  `{"type":"CLICK","row":..,"col":..}`, `{"type":"JUMP",...}` or
  `{"type":"RESTART"}`. The server replies with full game-state snapshots.
- **Rooms** — the room name is the URI path: `ws://localhost:8887/<room>`.
  A room is created on first connection and reused afterwards. The logged-in
  username travels as a query parameter: `?username=<name>`.
- **Threading** — the server is single-threaded with respect to game state.
  Commands arriving on network threads are *queued*; a single tick thread drains
  the queue, advances every game, and broadcasts the resulting snapshots. No game
  state is ever mutated from a network thread.
- **Authoritative server** — the client holds **no** `GameEngine` at all. It only
  sends clicks and renders whatever snapshot it receives. All rule validation,
  including rejecting a player's clicks on the opponent's pieces, happens server-side.
- **ELO** — when a king is captured, `GameLifecycleEvent(ENDED)` fires once on the
  event bus; the session then updates both players' ratings using the standard
  ELO formula with **K = 32** and persists them to SQLite.

---

## Testing

```bash
mvn clean test
```

Tests live in `src/test/java/texttests/` and are written with **JUnit 5**.
They cover the pure/logic layers — rules, board parsing, the protocol DTOs,
snapshot reconstruction, session/room behaviour, accounts and ELO — without
requiring a running server or a display. Test doubles (`FakeWebSocket`,
`RecordingGameClient`) stand in for real network objects.

---

## Known Limitations

- **Disconnect is not treated as a resignation.** If a player closes their window
  mid-game, the game simply waits — nobody loses and no rating changes. The freed
  color can be claimed by the next client that connects.
- **No matchmaking yet.** Players must agree on a room name manually; there is no
  automatic "find me an opponent" button.
- **Rooms have no dedicated UI.** Typing a room name creates it if missing and
  joins it if it exists — there are no explicit Create / Join / Cancel screens.
- **Server error messages are not surfaced in the game window.** Protocol errors
  are sent by the server and parsed by the client, but the Swing window currently
  ignores anything that isn't a snapshot.

---

## הוראות הרצה מקוצרות (עברית)

1. **דרישות**: Java 17 ו-Maven.
2. **להריץ את השרת** — הרצה של המחלקה `kfchess.net.server.ServerMain`.
   אמורה להופיע השורה `GameServer started on port 8887`. משאירים אותו רץ.
3. **להריץ את הלקוח** — הרצה של המחלקה **`kfchess.LoginScreenMain`**
   (זו נקודת הכניסה **היחידה** למשחק).
    - **Register** ליצירת חשבון חדש (דירוג התחלתי 1200) או **Login** לחשבון קיים.
    - במסך הבית מזינים שם חדר (או משאירים `default`) ולוחצים **Connect**.
    - הראשון שמתחבר לחדר מקבל **לבן**, השני **שחור**, כל השאר **צופים**.
4. **לשני שחקנים על אותו מחשב**: מריצים את `LoginScreenMain` פעמיים.
   ב-IntelliJ צריך לאפשר זאת: `Edit Configurations…` → `Modify options` →
   **Allow multiple instances**. מתחברים עם **שני חשבונות שונים** לאותו שם חדר
   (שני חשבונות שונים חשוב כדי שדירוג ה-ELO אכן יתעדכן).
5. **שליטה**: קליק שמאלי לבחירת כלי, קליק שמאלי נוסף ליעד, קליק ימני לקפיצה.
   בסיום המשחק מופיע כפתור **Restart** — **שני** הצדדים חייבים ללחוץ עליו
   כדי שהלוח יתאפס (עד אז מוצג "Waiting for opponent…").
6. **טסטים**: `mvn clean test`.