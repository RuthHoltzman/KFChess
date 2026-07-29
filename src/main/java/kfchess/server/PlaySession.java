package kfchess.server;

import kfchess.account.AccountRepository;
import kfchess.account.EloCalculator;
import kfchess.bus.EventBus;
import kfchess.bus.PlayLifecycleEvent;
import kfchess.engine.PlayCommandController;
import kfchess.engine.PlayEngine;
import kfchess.io.BoardParser;
import kfchess.model.Board;
import kfchess.model.PlayState;
import kfchess.model.PieceColor;
import kfchess.model.Position;
import kfchess.protocol.ClientCommand;
import kfchess.protocol.ClientCommandType;
import kfchess.model.ClientRole;
import kfchess.protocol.SnapshotMessage;
import kfchess.realtime.RaelTime;
import kfchess.rules.RuleEngine;
import org.java_websocket.WebSocket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * One game on the server: owns its own Board, PlayEngine and PlayCommandController, plus the
 * WebSocket connections taking part in it (white, black, spectators).
 * <p>
 * All game state changes happen on a single thread. Commands arriving on network threads are only
 * queued; the tick thread drains the queue and applies them - the standard real-time game server pattern.
 */
public class PlaySession {

    private static final String STARTING_BOARD_TEXT = """
            Board:
            bR bN bB bQ bK bB bN bR
            bP bP bP bP bP bP bP bP
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            wP wP wP wP wP wP wP wP
            wR wN wB wQ wK wB wN wR
            """;

    /** A command received from a connection, waiting to be applied on the next tick. */
    private record PendingCommand(WebSocket connection, ClientCommand command) {
    }

    /** A connection, the role it was given, and the username it identified with (null if no login). */
    private record ConnectedPlayer(ClientRole role, String username) {
    }

    /**
     * An open grace window for a WHITE/BLACK role whose connection dropped mid-game. Keeps the
     * username to recognize a reconnect, and the deadline on the game clock (not wall-clock).
     */
    private record PendingDisconnect(String username, long deadlineMillis) {
    }


    private static final long DISCONNECT_GRACE_MILLIS = 40_000;

    // How long a session may sit with nobody attached before the server drops it. Deliberately not
    // instant: a brand-new session is briefly empty between being created and its first role being
    // assigned, and this delay keeps the cleanup from racing with that.
    private static final long EMPTY_SESSION_TIMEOUT_MILLIS = 60_000;


    private Board board;
    private PlayEngine engine;
    // Routes CLICK/JUMP to the right color. Named after its class so it's obvious from the code
    // that this is the controller layer, not another engine.
    private PlayCommandController commandController;
    private final EventBus bus = new EventBus();
    private final AccountRepository accountRepository;
    private final Map<WebSocket, ConnectedPlayer> connections = new ConcurrentHashMap<>();
    // Only WHITE/BLACK can appear here - a spectator is dropped immediately, with no grace window.
    private final Map<ClientRole, PendingDisconnect> pendingDisconnects = new EnumMap<>(ClientRole.class);
    // Closed connections not yet processed. Like pendingCommands, the network thread only files them
    // here; the tick thread is the only one allowed to touch the engine in response.
    private final Queue<WebSocket> pendingDisconnections = new ConcurrentLinkedQueue<>();
    private final Queue<PendingCommand> pendingCommands = new ConcurrentLinkedQueue<>();
    // Who has already voted RESTART since the current game ended; cleared on every resetGame().
    // A Set, not a boolean, because we need "both sides voted", not just "someone voted".
    private final Set<ClientRole> restartVotes = EnumSet.noneOf(ClientRole.class);
    // Turns board+engine into a SnapshotMessage. Stateless, so one instance is safe for the session's whole life.
    private final SnapshotBuilder snapshotBuilder = new SnapshotBuilder();

    // Which board text resetGame() builds from - the real starting board in production, or a small
    // custom board injected by tests so a game can end in a single move.
    private final String boardText;

    // Wall clock moment this session became empty, or null while someone is still attached.
    // Wall clock (not the game clock) because the game clock restarts on every resetGame().
    private Long emptySinceMillis = System.currentTimeMillis();

    /** Convenience constructor with no account repository - a game without ELO updates, used by tests. */
    public PlaySession() {
        this(null);
    }

    /** Standard constructor: the real starting board, with ELO updates enabled. */
    public PlaySession(AccountRepository accountRepository) {
        this(STARTING_BOARD_TEXT, accountRepository);
    }

    /**
     * Full constructor, mainly for tests: injects the board text like the other collaborators.
     * The bus is created once per session (not per game) so the ELO subscription survives a restart.
     */
    public PlaySession(String boardText, AccountRepository accountRepository) {
        this.boardText = boardText;
        this.accountRepository = accountRepository;
        bus.subscribe(PlayLifecycleEvent.class, this::onGameLifecycleEvent);
        resetGame();
    }

    /**
     * Builds a fresh board, engine and controller - called from the constructor and again when both
     * sides vote RESTART. Connections, repository and bus belong to the session, so they are untouched.
     */
    private void resetGame() {
        this.board = new BoardParser(new Scanner(boardText)).readBoard();
        PlayState game = new PlayState(board);
        this.engine = new PlayEngine(game, new RuleEngine(), new RaelTime(), bus);
        this.commandController = new PlayCommandController(engine);
        restartVotes.clear();
    }

    /** Convenience overload for connections without a login (raw protocol testing). */
    public synchronized ClientRole assignRole(WebSocket connection) {
        return assignRole(connection, null);
    }

    /**
     * First to connect gets WHITE, second BLACK, everyone else SPECTATOR. Reconnects are checked
     * first, so a returning player gets their original role back instead of a new one.
     */
    public synchronized ClientRole assignRole(WebSocket connection, String username) {
        Optional<ClientRole> reconnected = tryReconnect(connection, username);
        if (reconnected.isPresent()) {
            return reconnected.get();
        }
        boolean whiteTaken = isRoleOccupied(ClientRole.WHITE);
        boolean blackTaken = isRoleOccupied(ClientRole.BLACK);
        ClientRole role = !whiteTaken ? ClientRole.WHITE : !blackTaken ? ClientRole.BLACK : ClientRole.SPECTATOR;
        connections.put(connection, new ConnectedPlayer(role, username));
        return role;
    }

    /** "Taken" means a live connection holds it, or it's reserved for someone still inside their grace window. */
    private boolean isRoleOccupied(ClientRole role) {
        return connections.values().stream().anyMatch(p -> p.role() == role) || pendingDisconnects.containsKey(role);
    }

    /**
     * Restores the original role if an open grace window matches this exact username.
     * A null username never counts as a reconnect, or anonymous connections would claim reserved seats.
     */
    private Optional<ClientRole> tryReconnect(WebSocket connection, String username) {
        if (username == null) {
            return Optional.empty();
        }
        for (Map.Entry<ClientRole, PendingDisconnect> entry : pendingDisconnects.entrySet()) {
            if (username.equals(entry.getValue().username())) {
                ClientRole role = entry.getKey();
                pendingDisconnects.remove(role);
                connections.put(connection, new ConnectedPlayer(role, username));
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    /** Drops a connection outright, with no grace-window logic - handleDisconnect is the real entry point. */
    public void removeConnection(WebSocket connection) {
        connections.remove(connection);
    }

    /** Called from the network thread: only queues the closed connection; processDisconnections does the work. */
    public void handleDisconnect(WebSocket connection) {
        pendingDisconnections.add(connection);
    }

    /** Called from the network thread: only queues the command, never touches the engine. */
    public void enqueueCommand(WebSocket connection, ClientCommand command) {
        pendingCommands.add(new PendingCommand(connection, command));
    }

    /**
     * The one place game state advances, on the tick thread only: process disconnects (before commands,
     * so a command from an already-closed connection can't slip through), apply queued commands,
     * advance the clock, then expire any grace window.
     */
    public synchronized void tick(long elapsedMillis) {
        processDisconnections();
        PendingCommand pending;
        while ((pending = pendingCommands.poll()) != null) {
            applyCommand(pending);
        }
        engine.handleWait(elapsedMillis);
        resolveExpiredDisconnects();
        updateEmptySince();
    }

    /** Tracks since when nobody has been attached - reset the moment anyone connects or reserves a seat. */
    private void updateEmptySince() {
        boolean occupied = !connections.isEmpty() || !pendingDisconnects.isEmpty();
        if (occupied) {
            emptySinceMillis = null;
        } else if (emptySinceMillis == null) {
            emptySinceMillis = System.currentTimeMillis();
        }
    }

    /**
     * Whether this session can be discarded: nobody connected, no reconnect window still open,
     * and it has been that way for longer than EMPTY_SESSION_TIMEOUT_MILLIS.
     */
    public synchronized boolean isAbandoned() {
        return emptySinceMillis != null
                && System.currentTimeMillis() - emptySinceMillis >= EMPTY_SESSION_TIMEOUT_MILLIS;
    }

    /**
     * Handles queued disconnects. A spectator, or anyone leaving after the game ended, is simply removed.
     * An active WHITE/BLACK keeps their role reserved with a deadline, so they can reconnect.
     */
    private void processDisconnections() {
        WebSocket connection;
        while ((connection = pendingDisconnections.poll()) != null) {
            ConnectedPlayer player = connections.get(connection);
            removeConnection(connection);
            if (player != null && player.role() != ClientRole.SPECTATOR && !engine.isGameOver()) {
                pendingDisconnects.put(player.role(),
                        new PendingDisconnect(player.username(), engine.now() + DISCONNECT_GRACE_MILLIS));
            }
        }
    }

    /**
     * Awards the win to the opponent once a grace window expires, through the same forceGameOver used
     * by king capture, so the existing ELO update applies without duplicating it.
     * Clearing happens after forceGameOver, so the ELO listener can still find the loser's username.
     */
    private void resolveExpiredDisconnects() {
        if (engine.isGameOver() || pendingDisconnects.isEmpty()) {
            return;
        }
        long now = engine.now();
        for (Map.Entry<ClientRole, PendingDisconnect> entry : pendingDisconnects.entrySet()) {
            if (now >= entry.getValue().deadlineMillis()) {
                PieceColor winner = entry.getKey().toPieceColor().orElseThrow().opposite();
                engine.forceGameOver(winner);
                pendingDisconnects.clear();
                return;
            }
        }
    }

    /**
     * Routes one command to the color of the connection that sent it; unknown senders and spectators are ignored.
     * While waiting for an opponent, CLICK/JUMP are silently dropped - RESTART is checked first since it
     * only applies once the game is already over.
     */
    private void applyCommand(PendingCommand pending) {
        ConnectedPlayer player = connections.get(pending.connection());
        if (player == null || !pending.command().isValid()) {
            return;
        }
        if (pending.command().type() == ClientCommandType.RESTART) {
            applyRestartVote(player.role());
            return;
        }
        if (isWaitingForOpponent()) {
            return;
        }
        player.role().toPieceColor().ifPresent(color -> {
            Position target = new Position(pending.command().row(), pending.command().col());
            switch (pending.command().type()) {
                case CLICK -> commandController.handleClick(color, target);
                case JUMP -> commandController.handleJump(color, target);
            }
        });
    }

    /**
     * Records one side's restart vote. Ignored before the game ends (otherwise a player could escape
     * a loss by resetting the board) and ignored for spectators. Resets only once both sides have voted.
     */
    private void applyRestartVote(ClientRole role) {
        if (!engine.isGameOver() || role == ClientRole.SPECTATOR) {
            return;
        }
        restartVotes.add(role);
        if (restartVotes.contains(ClientRole.WHITE) && restartVotes.contains(ClientRole.BLACK)) {
            resetGame();
        }
    }

    /**
     * True only when exactly one side is connected and the other seat is genuinely free - not merely
     * held open by a grace window, so a reconnecting player isn't replaced by a random matchmaker.
     */
    public synchronized boolean isWaitingForOpponent() {
        if (engine.isGameOver()) {
            return false;
        }
        boolean whiteConnected = connections.values().stream().anyMatch(p -> p.role() == ClientRole.WHITE);
        boolean blackConnected = connections.values().stream().anyMatch(p -> p.role() == ClientRole.BLACK);
        boolean whiteOpen = !whiteConnected && !pendingDisconnects.containsKey(ClientRole.WHITE);
        boolean blackOpen = !blackConnected && !pendingDisconnects.containsKey(ClientRole.BLACK);
        return (whiteConnected && blackOpen) || (blackConnected && whiteOpen);
    }

    /** The username of the lone waiting player, so matchmaking can compare ELO; empty if nobody is waiting. */
    public synchronized Optional<String> waitingPlayerUsername() {
        if (!isWaitingForOpponent()) {
            return Optional.empty();
        }
        return connections.values().stream().findFirst().map(ConnectedPlayer::username);
    }

    /** Defensive copy of the live connections and their roles - all PlayServer needs in order to broadcast. */
    public Map<WebSocket, ClientRole> connections() {
        Map<WebSocket, ClientRole> roles = new HashMap<>();
        connections.forEach((connection, player) -> roles.put(connection, player.role()));
        return roles;
    }

    /**
     * Fires exactly once when a game ends, and updates both players' ELO.
     * Skips silently when no meaningful rating can be computed: no repository, an unidentified side,
     * or both sides logged in as the same user.
     */
    private void onGameLifecycleEvent(PlayLifecycleEvent event) {
        if (accountRepository == null || event.phase() != PlayLifecycleEvent.Phase.ENDED) {
            return;
        }
        ClientRole winnerRole = event.winner() == PieceColor.WHITE ? ClientRole.WHITE : ClientRole.BLACK;
        ClientRole loserRole = winnerRole == ClientRole.WHITE ? ClientRole.BLACK : ClientRole.WHITE;
        String winnerUsername = usernameFor(winnerRole);
        String loserUsername = usernameFor(loserRole);
        if (winnerUsername == null || loserUsername == null || winnerUsername.equals(loserUsername)) {
            return;
        }

        accountRepository.currentElo(winnerUsername).ifPresent(winnerElo ->
                accountRepository.currentElo(loserUsername).ifPresent(loserElo -> {
                    int[] updated = EloCalculator.applyResult(winnerElo, loserElo);
                    accountRepository.updateElo(winnerUsername, updated[0]);
                    accountRepository.updateElo(loserUsername, updated[1]);
                }));
    }

    /**
     * The username currently holding a role, or null if nobody does.
     * Falls back to pendingDisconnects so a player who just timed out can still lose rating.
     */
    private String usernameFor(ClientRole role) {
        return connections.values().stream()
                .filter(player -> player.role() == role)
                .map(ConnectedPlayer::username)
                .findFirst()
                .or(() -> Optional.ofNullable(pendingDisconnects.get(role)).map(PendingDisconnect::username))
                .orElse(null);
    }

    /** Seconds until a disconnected player forfeits, or null if nobody is in a grace window. Same for every viewer. */
    private Integer disconnectSecondsRemaining() {
        if (pendingDisconnects.isEmpty()) {
            return null;
        }
        long now = engine.now();
        long soonestDeadline = pendingDisconnects.values().stream()
                .mapToLong(PendingDisconnect::deadlineMillis)
                .min()
                .orElseThrow();
        long remainingMillis = Math.max(0, soonestDeadline - now);
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    /**
     * Builds the snapshot for one viewer. Translating board+engine into a DTO lives in SnapshotBuilder;
     * what stays here is only the session-dependent detail (restart votes, disconnect countdown, waiting state).
     */
    public synchronized SnapshotMessage snapshotFor(ClientRole viewerRole) {
        // Only meaningful once the game is over; always false for a spectator, who has no restart vote.
        boolean restartRequestedByViewer = restartVotes.contains(viewerRole);
        return snapshotBuilder.build(board, engine, commandController, viewerRole,
                restartRequestedByViewer, disconnectSecondsRemaining(), isWaitingForOpponent());
    }
}
