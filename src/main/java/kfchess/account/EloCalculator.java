package kfchess.account;


public final class EloCalculator {

    private static final int K_FACTOR = 32;

    private EloCalculator() {
    }


    public static int[] applyResult(int winnerElo, int loserElo) {
        double expectedWinner = expectedScore(winnerElo, loserElo);
        double expectedLoser = 1.0 - expectedWinner;

        int newWinnerElo = (int) Math.round(winnerElo + K_FACTOR * (1.0 - expectedWinner));
        int newLoserElo = (int) Math.round(loserElo + K_FACTOR * (0.0 - expectedLoser));
        return new int[]{newWinnerElo, newLoserElo};
    }


    private static double expectedScore(int playerElo, int opponentElo) {
        return 1.0 / (1.0 + Math.pow(10, (opponentElo - playerElo) / 400.0));
    }
}
