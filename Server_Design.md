# KFChess - Server Design for Scale

**Status: design proposal, nothing implemented yet.** Written for the final CTD week, in answer to the
four questions posed by the instructor. Numbers below were computed from the actual message shapes in
`kfchess.protocol`, not estimated by feel - the arithmetic is shown so it can be argued with.

Target: **100M registered users, 10M concurrent players, a move every 2 seconds, games lasting 30-90s.**

---

## 0. Summary of the proposal

Split the single `PlayServer` process into five independently scalable tiers, move the live-game
routing table out of process memory into a shared directory, and stop broadcasting full snapshots.

```
                        [ global load balancer, per region ]
                                       |
     +---------------+---------------+---------------+
     |    Gateway    |    Gateway    |    Gateway    |     stateless, holds WebSockets
     +---------------+---------------+---------------+
                                       |
              +------------------------+------------------------+
              |                        |                        |
      +---------------+     +-------------------+     +-------------------+
      |  Matchmaker   |     |   Game Server     |     |  Account Service  |
      |  (ELO queues) |     |   (tick loop)     |     |  (login / ELO)    |
      +---------------+     +-------------------+     +-------------------+
              |                        |                        |
        +-----+------------------------+-----+                  |
        |        Redis Cluster              |                   |
        |  session directory + queues       |                   |
        +-----------------------------------+                   |
                                       |                        |
                             +---------------------+   +--------------------+
                             | Kafka: game results |-->| PostgreSQL (sharded)|
                             +---------------------+   +--------------------+
```

The single most important property we already have, and must protect: **sessions are completely
independent of one another.** No game shares state with any other game. That is what makes sharding a
routing problem rather than an engine rewrite.

---

## 1. Which database for 100M registered users?

### Is SQLite suitable? No.

Not because of size. 100M accounts is roughly:

| Column | Bytes |
|---|---|
| id | 16 |
| username | ~32 |
| bcrypt hash | 60 |
| elo | 4 |
| created_at, last_login | 16 |
| **row total** | **~128** |

100M x 128 B = **12.8 GB raw, ~28 GB with indexes and page overhead.** SQLite's file-size limit is
281 TB, so it would hold the data comfortably. Size is not the objection.

The real objections, in order of severity:

1. **One writer at a time, process-wide.** SQLite takes a write lock over the whole database file.
   Our write load at peak is ELO updates at game end: 10M players / 60s average game =
   **~167,000 ELO writes per second.** SQLite does low thousands of serialized writes/sec at best.
   Off by two orders of magnitude.

2. **It is a file, and containers do not share files.** `SqliteAccountRepository.DEFAULT_DB_FILE =
   "kfchess.db"` writes to the container's own filesystem, which is destroyed when the container
   stops. Two server containers would each silently get their own separate account database - you
   could register on one and fail to log in on the other. Putting the file on a shared network volume
   does not fix this: SQLite's locking is documented as unreliable over NFS/SMB.

3. **No replication and no failover.** One file on one disk. Losing that disk loses every account.

4. **No horizontal read scaling.** Login is our heaviest read path and there is nowhere to send the
   reads except the one file.

SQLite was the right call for a single-process bootcamp project - zero setup, zero ops, real SQL. It
stops being right the moment there is more than one process.

### The proposal: split by access pattern, not one database for everything

| Data | Store | Why |
|---|---|---|
| Accounts, password hashes, ELO | **PostgreSQL**, sharded by `hash(user_id)` | ACID matters for ELO; ~28 GB splits across 16-32 shards at ~1-2 GB each |
| Session directory (`gameId -> pod`), presence | **Redis Cluster** | Microsecond lookups on the hot path, and the data is disposable - if we lose it, the worst case is that in-flight games end |
| Matchmaking queues | **Redis sorted sets**, key = ELO | Replaces the O(n) scan; `ZRANGEBYSCORE elo-100 elo+100` is O(log n) |
| Game results, move history | **Kafka -> Postgres / object storage** | Write-behind; nobody is waiting on it |
| Live board state | **RAM only, never persisted** | See section 4 - it lives 60 seconds |

**Why Postgres over MySQL or Mongo:** we need transactional correctness on ELO (two rows updated
together at game end, and a lost update is a player's rating silently disappearing), and the schema is
small, stable and genuinely relational. Postgres is also the boring, well-understood option, which for
a datastore is a feature.

**The honest alternative:** our account access pattern is almost purely key-value - look up one row by
username, update one row by id. There are no joins. A wide-column store (Cassandra, DynamoDB) fits
that shape better and scales horizontally without us hand-rolling sharding. The reason to still choose
Postgres is that ELO updates want a transaction, and we would rather keep the transaction than the
convenience. This is a genuine trade-off and not an obvious call.

**On the 167,000 ELO writes/sec:** even sharded Postgres should not take those synchronously on the
game-end path. The game server publishes a `GameFinished` event to Kafka and returns immediately;
a pool of writer workers consumes the topic and applies ELO in **batched** transactions (say, 500
updates per statement). This turns 167k individual writes into a few hundred batched ones per second
per shard, and it decouples "the game ended" from "the database is having a bad day."

---

## 2. Is one server enough for 10M concurrent? How do we route?

### No - roughly 5,000 game-server pods

Today `PlayServer` advances **every** session on **one** thread (`tickExecutor` is a
`newSingleThreadScheduledExecutor`, 33 ms period). That caps us at a few hundred concurrent games
regardless of how many cores the machine has.

Even after partitioning sessions across a thread pool, a single pod with 4 cores handles on the order
of **1,000-2,000 concurrent games** (~2,000-4,000 players) before the tick loop misses its 33 ms
deadline. 10M players = 5M concurrent games, so:

> **~5,000 game-server pods**, plus gateway, matchmaker and account tiers on top.

The number is a planning estimate, not a measurement. Measuring it - how many games one pod really
sustains at 30 Hz - is the first benchmark worth building, because every other capacity number is
derived from it.

### Five tiers, because they scale on different axes

Splitting is not decoration. Each tier grows with a *different* input, so bundling them means
over-provisioning whichever one is not the bottleneck.

| Tier | Holds state? | Scales with | K8s object |
|---|---|---|---|
| **Gateway** | the WebSocket, nothing else | concurrent *connections* | Deployment + HPA |
| **Matchmaker** | no (queues live in Redis) | *login / queue* rate | Deployment |
| **Game server** | live board, in RAM | *active games* | Deployment + HPA, **no volume** |
| **Account service** | no | *login* rate | Deployment |
| **Result writers** | no | *games finished* per sec | Deployment (Kafka consumers) |

A player idling in the lobby costs a gateway slot and zero game-server CPU. A player mid-game costs
both. Today those are the same process, so we would have to scale the expensive thing to match the
cheap one.

### How do we know which player is on which server?

**We stop keeping that knowledge in any one process.** Today it is `PlayServer.sessions`, a
`ConcurrentHashMap` - invisible to every other JVM. It moves into Redis:

```
game:{gameId}   -> { pod: "gs-7f3a", region: "eu-west", players: [uid1, uid2], state: "ACTIVE" }
conn:{userId}   -> { gateway: "gw-22b1" }
pod:{podId}     -> { activeGames: 842, capacity: 1500, status: "READY" | "DRAINING" }
```

Joining a room, from the client's point of view unchanged:

```
1. client  -> nearest gateway        (WebSocket, TLS, JWT from login)
2. gateway -> Redis: GET game:{roomId}
3a. exists     -> gateway opens/reuses an internal connection to that pod, relays from now on
3b. not exists -> gateway -> matchmaker: pick a READY pod with capacity,
                  SETNX game:{roomId}, then relay
4. gateway relays client commands to the pod and pod deltas back to the client
```

**Why the gateway relays instead of redirecting the client to the game server:**

- Game servers need no public IP, no TLS certificate, and no exposure to the internet.
- A game server pod dying does not drop the player's connection - the gateway survives and can show
  "game aborted, requeueing" instead of a dead socket.
- Client-side reconnect logic stays trivial: one address, forever.

The cost is one extra network hop (~0.5 ms inside a datacenter) and a tier we have to operate. For a
game whose moves are 2 seconds apart, 0.5 ms is free.

### "Everyone can play with everyone", and any room is joinable

This falls out of the directory being shared. Any gateway can resolve any `gameId` because no gateway
owns the mapping. There is no "your room is on the server you happened to connect to."

**The regional caveat, stated honestly:** "everyone with everyone" is bounded by physics, not
architecture. Tokyo to São Paulo is ~250-300 ms round-trip. In a real-time game where a piece moves
over ~1 second, that is a quarter of the animation - the two players are watching measurably different
boards. So: **matchmaking is regional by default**, with the account database global, and cross-region
play offered only as an explicit "invite a friend" option with a latency warning. Pretending otherwise
would be designing for a number rather than for players.

### Fixing matchmaking

`resolveMatchmakingGameId` currently scans every waiting session under a global lock, which stalls
*every* new connection as room count grows. Replacement:

```
join:   ZADD   queue:{region}  {elo}  {userId}
match:  ZRANGEBYSCORE queue:{region} (elo-100) (elo+100) LIMIT 0 1
pair:   ZREM both, atomically, via a small Lua script
```

O(log n), no global lock, and it survives a matchmaker pod restart because the queue is in Redis
rather than in the pod. The ±100 ELO band and the 1-minute timeout carry over unchanged; on timeout we
widen the band instead of dropping the player.

---

## 3. How much network traffic does a move every 2 seconds cause?

### Inbound: negligible

A `ClientCommand` serializes to ~63 bytes; with WebSocket framing, TCP/IP and Ethernet headers, call
it ~123 bytes on the wire.

```
10,000,000 players x 0.5 moves/s x 123 B = 0.6 GB/s = 5 Gbps
```

**That is nothing.** A single modern server NIC is 25-100 Gbps. Spread over thousands of pods it is
invisible. If this were the whole problem there would be no problem.

### Outbound, as currently built: 9.5 Tbps

`PlayServer.broadcast` sends a **complete** `SnapshotMessage` to **every** connection **every tick**,
whether or not anything changed. Serialized with a 32-piece board and a 30-move log, that message is
**~3.9 KB**. At 30 ticks/second:

```
10,000,000 players x 30 msg/s x (3,898 + 60) B = 1,187 GB/s
                                               = 9.5 Tbps
```

**Is that a lot for the internet? It is an absurd amount.** For scale, that is in the neighbourhood of
a few percent of *all* global Netflix streaming, produced by a chess game. At typical cloud egress
pricing (~$0.02/GB) it costs about **$2.1 million per day**, or ~$770M/year, in bandwidth alone.

It also breaks a *single pod* long before it breaks the internet. 2,000 players on one pod:

```
2,000 x 30 x 3,958 B = 237 MB/s = 1.9 Gbps per pod
```

That exceeds a standard 1 Gbps NIC. **The pod is network-bound before it is CPU-bound** - we would be
paying for cores we cannot use, because the tick loop's output cannot get out of the machine.

Two compounding faults, both visible in the code:

- **`moveLog` is a `Map<String, List<String>>` sent in full every tick.** The snapshot therefore grows
  monotonically for the whole game. The client already has every one of those entries.
- **Nothing checks whether state changed.** A board where both players are thinking still emits 30
  identical 3.9 KB messages per second per player.

### Outbound, fixed: 0.01 Tbps

Send *events*, not *state*, and only when something happens:

```json
{"t":"MV","p":5,"to":36,"at":1753800000000,"d":1000}
```

52 bytes. The client already knows the board; it needs to be told what changed and when. Crucially,
**animation does not need packets**. A move event carries a start timestamp and a duration, and the
client interpolates the piece's travel locally - which is exactly what `Motion` and stable
`Piece.id()` already exist to support. Cooldowns are the same: send the expiry once, let the client
count down.

Each game generates ~1 event/second (two players, one move each per 2 s), delivered to both:

```
10,000,000 players x 1 msg/s x 112 B = 1.1 GB/s = 0.01 Tbps  (~$1.9K/day)
```

| | Full snapshot @30 Hz | Delta on change | Factor |
|---|---|---|---|
| Total outbound | 1,187 GB/s | 1.1 GB/s | **~1,060x** |
| Per pod (2,000 players) | 1.9 Gbps | 1.8 Mbps | ~1,060x |
| Egress cost | ~$2.1M/day | ~$1.9K/day | |

**Three orders of magnitude, and it is the single highest-value change in this document.** It is worth
doing even if we never run more than one server, because it is also what makes the game playable on a
phone network.

Keep a periodic full snapshot - say every 5 seconds, or on reconnect - as a correction against drift
and lost events. That is a rounding error on the delta figure and it removes a whole class of
"client and server disagree" bugs.

Further compression, if ever needed: binary encoding instead of JSON (~20 B/event), and WebSocket
`permessage-deflate`. Neither is worth doing before deltas; both are marginal afterwards.

---

## 4. Games last 30-90 seconds - what does that imply for the containers?

This is the subtlest of the four questions, and it is what actually determines the shape of the
deployment.

### 4.1 A container per game is the wrong answer

At 60 s average, 5M concurrent games means:

```
5,000,000 games / 60 s = ~83,000 games created and destroyed per second
```

Container startup is 100 ms - 1 s. 83,000 container starts per second is not a system, it is a denial
of service against our own cluster. **One pod hosts thousands of games; a game is an object in a map,
not a container.** Our current `PlaySession` model is already correct on this point - it just needs to
be one map per pod instead of one map in the world.

### 4.2 Live game state is never persisted

State that lives 60 seconds does not belong in a database. The board, cooldowns and in-flight motion
live in RAM and die there. Only the **result** - winner, ELO delta, optionally the move log - is
written, once, at the end, asynchronously through Kafka.

Concretely: the game-server tier needs **no PersistentVolumeClaim, and is a Deployment rather than a
StatefulSet.** Pods are interchangeable and disposable.

### 4.3 Failure is cheap, so we deliberately do not engineer around it

If a game-server pod dies, we lose at most 90 seconds of play for the games it held. Compare with a
database pod dying. That asymmetry is a design lever:

> **Decision: no replication or failover for live game state.** On pod loss, affected games are marked
> aborted with no ELO change, and both players are automatically re-queued.

Replicating live game state across pods would mean consensus on every tick - enormous complexity and
latency - to protect something worth 60 seconds. We accept a rare, bounded, quickly-recovered loss
instead. This is a conscious trade, recorded here so nobody later mistakes it for an oversight.

### 4.4 The short lifetime makes deploys and scale-down almost free

Because games end on their own within 90 seconds, a game-server pod can be retired **without
interrupting a single game**:

```
1. mark the pod DRAINING in the directory   -> matchmaker stops assigning new games to it
2. readiness probe goes false
3. wait ~90 s                               -> every remaining game finishes naturally
4. SIGTERM
```

In Kubernetes terms: a `preStop` hook that flips the directory entry, and
`terminationGracePeriodSeconds: 120`. A rolling deploy across the fleet interrupts nobody; it just
proceeds in 90-second waves.

**This property is exactly why the gateway must be a separate tier.** A gateway holds a player's
WebSocket for as long as they are online - 30 minutes, an hour. It cannot drain in 90 seconds.
Different state lifetime means a different upgrade strategy, different scaling signal, and therefore a
different Deployment. Bundling them would drag the gateway's hour-long connections into the game
server's 90-second lifecycle and lose the fast-drain property entirely.

### 4.5 What to autoscale on

Not CPU. The meaningful signal is **active games per pod**, exported as a custom metric:

```yaml
metrics:
  - type: Pods
    pods:
      metric: { name: active_games }
      target: { type: AverageValue, averageValue: "1000" }
```

Because games are short, the fleet tracks demand with roughly a two-minute lag and no user-visible
effect: new pods take new games, old pods empty themselves. Evening peaks and the follow-the-sun load
curve are handled by scaling the game tier while the gateway tier stays comparatively flat.

---

## 5. What this means for our code, concretely

Ordered by what blocks what.

| # | Change | Where | Why now |
|---|---|---|---|
| 1 | Externalize config to env vars | `ServerMain.DEFAULT_PORT`, `HomeScreen.SERVER_HOST_AND_PORT`, `SqliteAccountRepository.DEFAULT_DB_FILE` | Blocks Docker outright. The client can currently only ever reach `localhost`. |
| 2 | `Dockerfile` + `docker-compose` (server + Postgres) | new | Nothing below can be tested without it |
| 3 | **Delta protocol + send only on change** | `SnapshotBuilder`, `PlayServer.broadcast`, `ClientSnapshotReconstructor` | The 1,060x win. Also removes the per-tick `moveLog` resend. |
| 4 | Partition sessions across a thread pool | `PlayServer.tickAllSessions` | Lifts one pod from hundreds of games to thousands. Safe *because* sessions are independent - the "one thread mutates one engine" invariant is preserved by pinning each session to a fixed worker. |
| 5 | Swap SQLite for Postgres behind `AccountRepository` | `SqliteAccountRepository` | The interface already exists; this is why. Add `PostgresAccountRepository` alongside it. |
| 6 | Extract `ConnectionController` from `PlayServer` | `server` | Already noted in PROGRESS.md §5. Becomes urgent here: it is the seam along which the gateway/game-server split is made. |
| 7 | Redis session directory; matchmaking to sorted sets | `PlayServer.sessions`, `MatchmakingResolver` | The actual multi-pod step |
| 8 | Kafka + result writers for ELO | new | Only once write volume justifies it |

Items 1-5 are worth doing regardless of whether we ever run a second container. Items 6-8 are the
distributed system proper.

### Invariants that survive all of this

- **One thread mutates one `PlayEngine`.** Partitioning gives each session a fixed worker thread; it
  does not introduce sharing. The invariant gets *stronger* under sharding, not weaker.
- **Sessions stay independent.** Nothing above introduces cross-game state. If a proposal ever
  requires two games to coordinate, that is the signal to reject it.
- **`protocol` remains the single contract.** The delta message types belong there, defined once.

---

## 6. Open questions for review

1. Is 1,000-2,000 games per pod realistic? Everything else is derived from it and it is currently a
   guess. **Benchmark before building.**
2. Postgres with hand-rolled sharding, or Citus, or accept a KV store and give up the ELO
   transaction?
3. Do we keep move history at all? At 83,000 games/second it is by far the largest data volume in the
   system, and nothing in the product currently reads it back.
4. Is aborting games on pod loss acceptable to the reviewers, or is some reconnect-and-resume
   expected?
5. Regional matchmaking contradicts a literal reading of "everyone plays with everyone." Which
   interpretation did the instructor intend?

---

## 7. Sources of the numbers

Everything in section 3 was computed from the real serialized shape of `SnapshotMessage` and
`ClientCommand` (32 `PieceDto`s, a 30-entry `moveLog`, one active `Motion`), plus 60 bytes of
WebSocket + TCP/IP + Ethernet overhead per message, at the `TICK_INTERVAL_MILLIS = 33` tick rate that
`PlayServer` actually uses. Egress priced at $0.02/GB. The capacity figure of 1,000-2,000 games per
pod is the one number that is estimated rather than derived, and item 1 in section 6 exists to
replace it.
