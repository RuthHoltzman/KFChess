# KFChess - Project Handoff

**Audience: an AI assistant starting a fresh session on this repo.** Read this first; it should be
enough to start working without scrolling any chat history. Development history is intentionally not
kept here - use `git log` for that.

---

## 1. What this is

A real-time chess variant ("kung-fu chess"): pieces move simultaneously, with per-piece cooldowns
instead of turns. Client/server over WebSocket, Swing UI on the client.

Built as a bootcamp project. **The stated goal is learning and an architecture that could in
principle scale to a million users** - not merely something that runs on one laptop. Architectural
critique is welcome and expected.

**Stack:** Java 17, Maven, JUnit 5, Gson 2.14, Java-WebSocket 1.6, sqlite-jdbc, jBCrypt, Swing.

### Build, run, test

| Task | Command / entry point |
|---|---|
| Tests | `mvn test` (161 tests, all green) |
| Start server | `kfchess.server.ServerMain` (optional port arg, default 8887) |
| Start client | `kfchess.app.LoginScreenMain` (one JVM per player) |

Two players = two separate client JVMs. In IntelliJ, enable "Allow multiple instances".

---

## 2. Package map

Dependencies point **downward only**. Nothing lower ever imports something higher.

```
app                    entry points, window wiring          (LoginScreenMain, HomeScreen, NetworkPlayWindow)
 |-- server            WebSocket server, sessions           (PlayServer, PlaySession, SnapshotBuilder, *Resolver)
 |-- client            WebSocket client, snapshot decoding  (PlayClient, ClientSnapshotReconstructor, NetworkClickHandler)
 |-- view              rendering                            (Img, BoardView, PlaySceneView, SidePanelView, view.layout)
 |-- protocol          the client/server contract (DTOs)    (ClientCommand, SnapshotMessage, ConnectionPaths, ...)
 |-- engine            game orchestration                   (PlayEngine, PlayCommandController, PieceTimers, MoveHistory)
 |    \-- engine.snapshot  domain state -> render DTO       (SnapshotFactory, PlaySnapshot, PieceVisualState, ...)
 |-- rules             move legality                        (RuleEngine, PieceRules)
 |-- realtime          game clock and in-flight motion      (RaelTime, Motion)
 |-- bus               in-process pub/sub                   (EventBus, *Event)
 |-- account           accounts, ELO, SQLite                (AccountRepository, EloCalculator, PasswordHasher)
 |-- io                board-text parsing                   (BoardParser)
 |-- input             pixel -> board square                (BoardMapper)
 |-- logging           operational file log                 (FileLogger)
 \-- model             pure domain, no I/O                  (Board, Piece, Position, Game, PieceColor/Kind/State, ClientRole)
```

### Request flow

```
click -> NetworkClickHandler -> PlayClient --JSON--> PlayServer.onMessage
                                                          | (queues only)
                                                          v
                                                PlaySession.tick()   <- single tick thread, ~30/s
                                                          |
                                        PlayCommandController -> PlayEngine -> rules / timers / bus
                                                          |
                                                SnapshotBuilder -> SnapshotMessage
                                                          |
PlaySceneView <- SnapshotFactory <- ClientSnapshotReconstructor <--JSON-- broadcast
```

---

## 3. Invariants - do not break these

1. **One thread mutates game state.** Network threads (`onOpen`/`onMessage`/`onClose`) only enqueue
   into `pendingCommands` / `pendingDisconnections`. Only `PlaySession.tick()`, on the single tick
   thread, touches a `PlayEngine`. Violating this reintroduces races that are very hard to find.

2. **Swing only on the EDT.** Anything reaching Swing from a network thread must go through
   `SwingUtilities.invokeLater`. `PlayClient`'s message listener already does this.

3. **Dependencies point downward.** See the map above. In particular `model`, `rules` and `realtime`
   know nothing about networking, Swing or persistence, which is what keeps them unit-testable.

4. **`PlayCommandController` lives in `engine`, deliberately.** It needs package-private access to
   `PlayEngine` (`tryMove`, `beginJump`, `isAvailableToAct`, `advanceGameState`). Moving it into
   `server` would force those methods public and break the engine's encapsulation. This trade was
   made consciously: slightly less obvious package naming, in exchange for a smaller public surface.

5. **The game clock is injected** (`RaelTime`), never `System.currentTimeMillis()` inside the engine.
   That is what lets tests jump 45 seconds forward in one `tick(45_000)` call with no sleeping.
   Exception: `PlayServer` matchmaking deadlines and `PlaySession`'s empty-session timer use wall
   clock on purpose - they are server lifecycle, not game logic.

6. **Piece identity survives the network.** `Piece.id()` (AtomicLong) is how
   `ClientSnapshotReconstructor` recognizes "same piece as last message" across separate JSON
   decodes. Without it, animations restart every frame.

7. **`protocol` is the shared contract.** Anything both ends must agree on belongs there and must
   have exactly one definition site - see `ConnectionPaths`.

8. **Comments are short, one-line, English.** One line above each meaningful function saying what it
   does. The whole repo was converted to this style; keep it.

---

## 4. Current status

All six assignment stages are implemented, manually verified, and committed:

1. Event bus (pub/sub)
2. Single-threaded WebSocket server + full Swing network client
3. Home screen
4. Accounts + ELO (SQLite, bcrypt, K=32) - plus mutual Restart (both sides must agree)
5. Matchmaking ("Play", ELO +/-100, 1-minute timeout) + disconnect auto-resign with a grace window
6. Rooms (Create / Join / Cancel) + operational logs on both sides

`mvn test` is green (161 tests). No known failing behavior.

Recent maintenance work, already done:

- All Hebrew comments across `src/main` and `src/test` replaced with short English ones.
- Dead code removed: `PlayEngine.handleClick/handleJump/selectedPosition`, `IncomingSnapshot.type()`,
  `HomeScreen.buildUri(String)`, an unused 14-arg `SnapshotMessage` constructor,
  `AnimationClip.frameCount()`.
- `NetworkActions` renamed to `PlayCommandController`; `SnapshotBuilder` extracted from `PlaySession`.
- Client rendering switched from a fixed 60fps Swing `Timer` to event-driven - a server message or a
  window resize triggers the repaint.
- Abandoned sessions are now discarded: previously every room ever created stayed in memory and kept
  being ticked 30 times a second forever.
- `Motion` converted to a record.
- Shared connection-path tokens centralized in `protocol.ConnectionPaths`.

---

## 5. Next stage: Docker and scalability

This is where work resumes. Everything below was analyzed and deliberately deferred until Docker.

**What is already right** (and worth protecting): sessions are fully independent of one another -
no shared state between games. That is the single most important property for scaling, and it means
games can be sharded across processes without touching game logic.

**Hard limits, in priority order:**

| # | Limit | Where | Note |
|---|---|---|---|
| 1 | One tick thread advances *all* sessions sequentially | `PlayServer.tickAllSessions` | The main bottleneck. Current ceiling is roughly a few hundred concurrent games. Easy to fix precisely because sessions are independent - partition them across a thread pool. |
| 2 | Full snapshot broadcast to every connection every tick | `PlayServer.broadcast` | Sent even when nothing changed. At scale this is the bandwidth killer. Fix: deltas, and only when state actually changed. |
| 3 | Matchmaking is an O(n) scan under a global lock | `PlayServer.resolveMatchmakingGameId` | Stalls every new connection once there are many rooms. Fix: an ELO-bucketed waiting queue. |
| 4 | Single JVM; `sessions` is an in-memory map | `PlayServer` | No horizontal scaling. Needs sharding by gameId plus a routing layer. This is the Docker conversation. |
| 5 | SQLite, single-writer file | `SqliteAccountRepository` | Fine for one process, breaks with several servers. Would need Postgres or similar. |
| 6 | Hardcoded deployment config | see below | Blocks Docker directly. |

**Config that must be externalized before Docker** (currently hardcoded):

| Value | Location |
|---|---|
| `8887` | `ServerMain.DEFAULT_PORT` |
| `"ws://localhost:8887"` | `HomeScreen.SERVER_HOST_AND_PORT` |
| `"kfchess.db"` | `SqliteAccountRepository.DEFAULT_DB_FILE` |

The client address is the blocking one: today the client can only ever reach `localhost`.

**Also open, from code review:** extracting a `ConnectionController` out of `PlayServer`.
`PlayServer` currently mixes transport (extending `WebSocketServer`, the tick loop) with dispatch
(choosing Create/Play/Join, routing messages). The dispatch half is untestable as a result - `onOpen`
needs a real handshake. The `*Resolver` classes are already this pattern; what remains unextracted is
the orchestration between them. A real improvement in testability, not urgent.

---

## 6. Known limitations, accepted on purpose

- **Restart after auto-resign is unreachable.** Mutual restart needs both sides to vote, but when a
  game ends by disconnect one side is gone. Not classed as a bug; if it ever matters, let the
  remaining player start a new game alone when `gameOver` was caused by a disconnect.
- **A room literally named `_play` or `_create` collides** with the reserved matchmaking/create
  paths. Rare enough to leave alone.
- **`RuleEngine.registerCustomRule`** is exercised only by tests. Kept as a deliberate extension
  point, not dead code.
- **No JaCoCo.** Coverage is unmeasured; the instructor asked for 80%+. `view/` and the entry-point
  classes have no dedicated tests, so the real figure is likely below that. Adding the plugin was
  discussed and never decided.
- **ELO +/-100 matching is implemented but never manually verified** (needs two accounts with
  deliberately distant ratings). The 1-minute timeout path *was* verified.

---

## 7. Working agreements with the developer

These are not preferences to infer - they were stated explicitly and they matter.

- **Never implement on the strength of an opinion.** "I think it would be good if..." is thinking out
  loud, not an instruction. Wait for an explicit go-ahead. This was violated once and caused real
  friction. When in doubt, describe the plan and stop.
- **Explain every change in chat**, function by function - not only in code comments.
- **Never delete anything without asking first.** Report what looks dead, with evidence (grep across
  `src/main` and `src/test`), then wait.
- **Comments: one short English line per meaningful function.** Not paragraphs. This was corrected
  explicitly once.
- **Offer an exact commit message after each unit of work.** Short, describing *what* was done, not
  *how*. Never run git commit - she always does. `git status` / `git diff` are fine.
- **She runs `mvn test` herself.** There is no javac or Maven in the assistant's sandbox, only a JRE.
  Verify structurally instead: brace balance, package/class-name consistency, unresolved `kfchess.*`
  imports, leftover references after a rename. A Python script over `src/` is the established way.
- **She edits files in IntelliJ concurrently.** If an edit fails with "file has been modified",
  re-read the file before retrying - and consider that she may have changed logic, not just
  formatting.
- **She writes in Hebrew and expects Hebrew replies.** Code, comments and this file are English.
- She is reading the code line by line to understand it. Prefer explaining clearly over being clever,
  and prefer small focused classes over dense ones.
