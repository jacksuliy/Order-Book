package engine;

/**
 * Small command-line demo: places a few orders and prints the resulting trades and top of book.
 */
public final class Demo {
    private Demo() { }

    /**
     * Runs the demo.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        OrderBook book = new OrderBook();
        book.submitLimit(1, Side.SELL, 10_100, 50);
        book.submitLimit(2, Side.SELL, 10_050, 30);
        book.submitLimit(3, Side.BUY, 10_000, 40);
        System.out.println("Best bid " + book.bestBid() + ", best ask " + book.bestAsk());

        // Aggressive buy sweeps the 10_050 level and part of 10_100.
        for (Trade t : book.submitLimit(4, Side.BUY, 10_100, 60)) System.out.println(t);
        System.out.println("Best bid " + book.bestBid() + ", best ask " + book.bestAsk());
    }
}
