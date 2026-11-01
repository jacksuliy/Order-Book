package engine;

/**
 * An order and its current state. Instances are created and mutated only by {@link OrderBook};
 * the public accessors are read-only.
 *
 * <p>Prices are stored as {@code long} ticks rather than {@code double} to avoid
 * floating-point rounding error.
 */
public final class Order {
    final long id;
    final Side side;
    final long price;
    final boolean isMarket;
    long remaining;

    /**
     * Creates an order.
     *
     * @param id       unique order id
     * @param side     buy or sell
     * @param price    limit price in ticks; ignored for market orders
     * @param quantity initial quantity
     * @param isMarket {@code true} for a market order, {@code false} for a limit order
     */
    Order(long id, Side side, long price, long quantity, boolean isMarket) {
        this.id = id;
        this.side = side;
        this.price = price;
        this.remaining = quantity;
        this.isMarket = isMarket;
    }

    /**
     * Returns this order's unique id.
     *
     * @return the order id
     */
    public long id() { return id; }

    /**
     * Returns the side of this order.
     *
     * @return {@link Side#BUY} or {@link Side#SELL}
     */
    public Side side() { return side; }

    /**
     * Returns the limit price of this order.
     *
     * @return the limit price in ticks, or 0 for a market order
     */
    public long price() { return price; }

    /**
     * Returns the quantity still unfilled.
     *
     * @return remaining quantity
     */
    public long remaining() { return remaining; }
}
