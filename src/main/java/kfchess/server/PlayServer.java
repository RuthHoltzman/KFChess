package kfchess.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import kfchess.account.AccountRepository;
import kfchess.account.SqliteAccountRepository;
import kfchess.logging.FileLogger;
import kfchess.protocol.ClientCommand;
import kfchess.model.ClientRole;
import kfchess.protocol.ErrorMessage;
import kfchess.protocol.MatchmakingTimeoutMessage;
import kfchess.protocol.RoleAssignedMessage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The network entry point: holds one PlaySession per gameId and runs the single tick thread that
 * advances and broadcasts every game.
 * <p>
 * onOpen/onMessage/onClose run on the library's network threads and only file commands or update
 * connection maps - they never touch a PlayEngine directly.
 */
public class PlayServer extends WebSocketServer {

    private static final long TICK_INTERVAL_MILLIS = 33; // ~30 updates per second
    private static final int ELO_MATCH_RANGE = 100;
    private static final long MATCHMAKING_TIMEOUT_MILLIS = 60_000;

    private final Gson gson = new Gson();
    // One repository shared by every session (and by the login screen) - the same kfchess.db file,
    // because these are the same accounts.
    private final AccountRepository accountRepository =
            new SqliteAccountRepository(SqliteAccountRepository.DEFAULT_DB_FILE);
    private final Map<String, PlaySession> sessions = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> gameIdByConnection = new ConcurrentHashMap<>();
    // Sessions created through Play. Without this, matchmaking could steal a session that is waiting
    // for a specific friend to Join a private room - Play must only match other Play players.
    private final Set<String> matchmakingSessionIds = ConcurrentHashMap.newKeySet();
    // gameId -> wall-clock moment its 1-minute matchmaking timeout expires. Removed once a match is
    // found or the timeout has already fired.
    private final Map<String, Long> matchmakingDeadlines = new ConcurrentHashMap<>();
    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();
    // Guards "decide which gameId to use". Both matchmaking and Create room share it, because both
    // solve the same problem: find or invent a free gameId and claim it before anyone else does.
    private final Object sessionAllocationLock = new Object();
    // Operational log for the whole server process (not per session) - unrelated to the chess move log.
    private final FileLogger fileLogger = new FileLogger("server");
    private long lastTickNanos = System.nanoTime();

    public PlayServer(int port) {
        super(new InetSocketAddress(port));
    }

    /** Called once the socket is open: starts the tick loop on its own thread, separate from the network threads. */
    @Override
    public void onStart() {
        lastTickNanos = System.nanoTime();
        tickExecutor.scheduleAtFixedRate(this::tickAllSessions, 0, TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        System.out.println("PlayServer started on port " + getPort());
        fileLogger.log("PlayServer started on port " + getPort());
    }

    /**
     * New connection: picks the game from the request path (Create / Play / Join), assigns a role,
     * records the username if one was supplied, and tells the client immediately.
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String path = handshake.getResourceDescriptor();
        // Resolved before the gameId, because matchmaking needs the username to compare ELO.
        String username = UsernameResolver.resolve(path).orElse(null);
        String gameId;
        String connectionMethod;
        if (CreateRoomResolver.isCreateRoomRequest(path)) {
            gameId = createNewRoomGameId();
            connectionMethod = "Create";
        } else if (MatchmakingResolver.isMatchmakingRequest(path)) {
            gameId = resolveMatchmakingGameId(username);
            connectionMethod = "Play";
        } else {
            gameId = PlayIdResolver.resolve(path);
            connectionMethod = "Join";
        }
        PlaySession session = sessions.computeIfAbsent(gameId, id -> new PlaySession(accountRepository));
        gameIdByConnection.put(conn, gameId);
        ClientRole role = session.assignRole(conn, username);
        fileLogger.log("Connection opened via " + connectionMethod + ": gameId=" + gameId
                + ", role=" + role + ", username=" + (username == null ? "-" : username));
        conn.send(gson.toJson(new RoleAssignedMessage(role.name(), gameId)));
    }

    /**
     * Finds a waiting session with a compatible ELO and joins it, or creates a new one with a
     * unique "match-&lt;uuid&gt;" id and a one-minute deadline.
     * <p>
     * The lock is required: without it two near-simultaneous searchers could both find nothing,
     * both create their own session, and never meet.
     */
    private String resolveMatchmakingGameId(String searcherUsername) {
        Integer searcherElo = eloFor(searcherUsername).orElse(null);
        synchronized (sessionAllocationLock) {
            for (Map.Entry<String, PlaySession> entry : sessions.entrySet()) {
                PlaySession candidate = entry.getValue();
                if (!matchmakingSessionIds.contains(entry.getKey()) || !candidate.isWaitingForOpponent()) {
                    continue;
                }
                Integer candidateElo = eloFor(candidate.waitingPlayerUsername().orElse(null)).orElse(null);
                if (isCompatibleElo(searcherElo, candidateElo)) {
                    matchmakingDeadlines.remove(entry.getKey()); // matched - cancel the timeout
                    return entry.getKey();
                }
            }
            String newGameId = "match-" + UUID.randomUUID();
            matchmakingSessionIds.add(newGameId);
            matchmakingDeadlines.put(newGameId, System.currentTimeMillis() + MATCHMAKING_TIMEOUT_MILLIS);
            sessions.computeIfAbsent(newGameId, id -> new PlaySession(accountRepository));
            return newGameId;
        }
    }

    /** The current ELO for a username, or empty if there's no username (anonymous) or no such account. */
    private Optional<Integer> eloFor(String username) {
        return username == null ? Optional.empty() : accountRepository.currentElo(username);
    }

    /**
     * Whether two ratings are within +/-100 - or whether there simply isn't enough information to judge.
     * That lenient fallback is deliberate: without it, anonymous connections could never be matched at all.
     */
    private boolean isCompatibleElo(Integer searcherElo, Integer candidateElo) {
        if (searcherElo == null || candidateElo == null) {
            return true;
        }
        return Math.abs(searcherElo - candidateElo) <= ELO_MATCH_RANGE;
    }

    /**
     * Called each tick for sessions with a registered deadline: clears the entry if someone joined,
     * otherwise sends MATCHMAKING_TIMEOUT once to the lone waiting connection.
     */
    private void checkMatchmakingTimeout(String gameId, PlaySession session) {
        Long deadline = matchmakingDeadlines.get(gameId);
        if (deadline == null) {
            return;
        }
        if (!session.isWaitingForOpponent()) {
            matchmakingDeadlines.remove(gameId);
            return;
        }
        if (System.currentTimeMillis() < deadline) {
            return;
        }
        matchmakingDeadlines.remove(gameId);
        MatchmakingTimeoutMessage timeoutMessage = new MatchmakingTimeoutMessage(
                "Could not find a match with a compatible ELO within 1 minute.");
        session.connections().keySet().forEach(conn -> conn.send(gson.toJson(timeoutMessage)));
        fileLogger.log("Matchmaking timed out: gameId=" + gameId);
    }

    /**
     * Invents a short, human-readable room code (unlike the matchmaking UUID, this one gets spoken
     * and typed between people) and opens a session for it.
     */
    private String createNewRoomGameId() {
        synchronized (sessionAllocationLock) {
            String newGameId;
            do {
                newGameId = RoomIdGenerator.generate();
            } while (sessions.containsKey(newGameId));
            sessions.computeIfAbsent(newGameId, id -> new PlaySession(accountRepository));
            return newGameId;
        }
    }

    /** Disconnect: handed to PlaySession, which gives an active player a grace window to reconnect. */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String gameId = gameIdByConnection.remove(conn);
        fileLogger.log("Connection closed: gameId=" + gameId + ", code=" + code + ", reason=" + reason);
        PlaySession session = gameId == null ? null : sessions.get(gameId);
        if (session != null) {
            session.handleDisconnect(conn);
        }
    }

    /** Incoming message: only decodes and queues it - the command is actually applied on the tick thread. */
    @Override
    public void onMessage(WebSocket conn, String message) {
        PlaySession session = sessions.get(gameIdByConnection.get(conn));
        if (session == null) {
            return;
        }
        try {
            ClientCommand command = gson.fromJson(message, ClientCommand.class);
            if (command == null || !command.isValid()) {
                conn.send(gson.toJson(new ErrorMessage("invalid command: " + message)));
                return;
            }
            session.enqueueCommand(conn, command);
        } catch (JsonSyntaxException malformedJson) {
            conn.send(gson.toJson(new ErrorMessage("malformed JSON: " + malformedJson.getMessage())));
        }
    }

    /** Network error - logged only; a dropped connection is handled separately by onClose. */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("PlayServer error: " + ex.getMessage());
        fileLogger.log("ERROR: " + ex.getMessage());
    }

    /** Runs only on the tick thread: advances every session by the elapsed time, checks matchmaking timeouts, then broadcasts. */
    private void tickAllSessions() {
        long now = System.nanoTime();
        long elapsedMillis = (now - lastTickNanos) / 1_000_000;
        lastTickNanos = now;

        for (Map.Entry<String, PlaySession> entry : sessions.entrySet()) {
            PlaySession session = entry.getValue();
            session.tick(elapsedMillis);
            if (!matchmakingDeadlines.isEmpty()) {
                checkMatchmakingTimeout(entry.getKey(), session);
            }
            broadcast(session);
            removeIfAbandoned(entry.getKey(), session);
        }
    }

    /**
     * Drops a session once everyone has left it, so finished rooms stop consuming memory and tick time.
     * <p>
     * The removal is re-checked inside computeIfPresent, which holds the same per-key lock as the
     * computeIfAbsent in onOpen - so a session cannot be discarded while a new connection is claiming it.
     */
    private void removeIfAbandoned(String gameId, PlaySession session) {
        if (!session.isAbandoned()) {
            return;
        }
        synchronized (sessionAllocationLock) {
            sessions.computeIfPresent(gameId, (id, existing) -> existing.isAbandoned() ? null : existing);
        }
        matchmakingSessionIds.remove(gameId);
        matchmakingDeadlines.remove(gameId);
        fileLogger.log("Session discarded (empty): gameId=" + gameId);
    }

    /** Sends each connection a snapshot tailored to its role; a already-closed connection is skipped. */
    private void broadcast(PlaySession session) {
        session.connections().forEach((conn, role) -> {
            try {
                conn.send(gson.toJson(session.snapshotFor(role)));
            } catch (RuntimeException sendFailed) {
                // Connection already broken - ignore it, onClose is on its way.
            }
        });
    }
}
