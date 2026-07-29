package kfchess.server;


/** Entry point that starts the game server, optionally on a port given as the first argument. */
public final class ServerMain {

    private static final int DEFAULT_PORT = 8887;

    private ServerMain() {
    }

    /** Starts the server on the given port, or the default one. */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new PlayServer(port).start();
    }
}
