package engine;

import java.util.*;

/**
 * A single-instrument limit order book with price-time priority matching.
 *
 * <p>Bids are held in a {@link TreeMap} sorted from highest to lowest price and asks in one
 * sorted from lowest to highest, so the best price on each side is always the first key.
 * Each price level is a FIFO queue, so earlier orders at the same price fill first.
 * A {@link HashMap} from order id to order gives O(1) lookup for cancels.
 *
 * <p>This class is not thread-safe.
 */
public final class OrderBook {
    private final TreeMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
    private final Map<Long, Order> live = new HashMap<>();

    /**
     * Creates an empty order book.
     */
    public OrderBook() { }

    /**
     * Submits a limit order. The order first matches against the opposite side at prices
     * equal to or better than its limit; any unfilled remainder rests on the book.
     *
     * @param id       unique order id; must not belong to an order currently on the book
     * @param side     buy or sell
     * @param price    limit price in ticks; must be positive
     * @param quantity quantity to trade; must be positive
     * @return the trades generated, in execution order (empty if nothing matched)
     * @throws IllegalArgumentException if the id is a duplicate, or price or quantity is not positive
     */
    public List<Trade> submitLimit(long id, Side side, long price, long quantity) {
        validate(id, quantity);
        if (price <= 0) throw new IllegalArgumentException("price must be positive");
        Order order = new Order(id, side, price, quantity, false);
        List<Trade> trades = match(order);
        if (order.remaining > 0) rest(order);
        return trades;
    }

    /**
     * Submits a market order. It fills against the best available prices until it is
     * complete or the opposite side is empty. Any unfilled remainder is discarded and never rests.
     *
     * @param id       unique order id; must not belong to an order currently on the book
     * @param side     buy or sell
     * @param quantity quantity to trade; must be positive
     * @return the trades generated, in execution order (empty if the opposite side was empty)
     * @throws IllegalArgumentException if the id is a duplicate or quantity is not positive
     */
    public List<Trade> submitMarket(long id, Side side, long quantity) {
        validate(id, quantity);
        return match(new Order(id, side, 0, quantity, true));
    }

    /**
     * Cancels an order resting on the book. Removes its price level if the level becomes empty.
     *
     * <p>Runs in O(log n + k), where k is the number of orders at that price level.
     *
     * @param id id of the order to cancel
     * @return {@code true} if the order was found and removed; {@code false} if it was not on the book
     */
    public boolean cancel(long id) {
        Order order = live.remove(id);
        if (order == null) return false;
        TreeMap<Long, ArrayDeque<Order>> book = bookFor(order.side);
        ArrayDeque<Order> level = book.get(order.price);
        level.remove(order);
        if (level.isEmpty()) book.remove(order.price);
        return true;
    }

    /**
     * Returns the highest bid price.
     *
     * @return the best bid in ticks, or an empty {@link OptionalLong} if there are no bids
     */
    public OptionalLong bestBid() { return bids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(bids.firstKey()); }

    /**
     * Returns the lowest ask price.
     *
     * @return the best ask in ticks, or an empty {@link OptionalLong} if there are no asks
     */
    public OptionalLong bestAsk() { return asks.isEmpty() ? OptionalLong.empty() : OptionalLong.of(asks.firstKey()); }

    /**
     * Returns the total resting quantity at one price on one side of the book.
     *
     * @param side  the side to inspect
     * @param price the price level in ticks
     * @return total remaining quantity at that level, or 0 if the level is empty
     */
    public long depthAt(Side side, long price) {
        ArrayDeque<Order> level = bookFor(side).get(price);
        if (level == null) return 0;
        long total = 0;
        for (Order o : level) total += o.remaining;
        return total;
    }

    /**
     * Reports whether an order is currently resting on the book.
     *
     * @param id the order id
     * @return {@code true} if the order is on the book with quantity remaining
     */
    public boolean isResting(long id) { return live.containsKey(id); }

    /**
     * Matches an incoming order against the opposite side, best price first and FIFO within
     * each level, until the order is filled or no longer crosses.
     *
     * @param incoming the order being matched; its remaining quantity is reduced in place
     * @return the trades generated
     */
    private List<Trade> match(Order incoming) {
        List<Trade> trades = new ArrayList<>();
        TreeMap<Long, ArrayDeque<Order>> opposite = incoming.side == Side.BUY ? asks : bids;

        while (incoming.remaining > 0 && !opposite.isEmpty()) {
            Map.Entry<Long, ArrayDeque<Order>> best = opposite.firstEntry();
            long levelPrice = best.getKey();
            if (!crosses(incoming, levelPrice)) break;

            ArrayDeque<Order> level = best.getValue();
            while (incoming.remaining > 0 && !level.isEmpty()) {
                Order resting = level.peekFirst();
                long qty = Math.min(incoming.remaining, resting.remaining);
                incoming.remaining -= qty;
                resting.remaining -= qty;
                trades.add(new Trade(incoming.id, resting.id, incoming.side, levelPrice, qty));
                if (resting.remaining == 0) {
                    level.pollFirst();
                    live.remove(resting.id);
                }
            }
            if (level.isEmpty()) opposite.remove(levelPrice);
        }
        return trades;
    }

    /**
     * Determines whether an incoming order can trade at a given opposite-side price.
     *
     * @param incoming   the incoming order
     * @param levelPrice the best price on the opposite side
     * @return {@code true} if the order is a market order or its limit reaches {@code levelPrice}
     */
    private static boolean crosses(Order incoming, long levelPrice) {
        if (incoming.isMarket) return true;
        return incoming.side == Side.BUY ? incoming.price >= levelPrice : incoming.price <= levelPrice;
    }

    /**
     * Adds an order to the back of its price level and registers it for lookup.
     *
     * @param order the order to rest
     */
    private void rest(Order order) {
        bookFor(order.side).computeIfAbsent(order.price, p -> new ArrayDeque<>()).addLast(order);
        live.put(order.id, order);
    }

    /**
     * Returns the price-level map for one side of the book.
     *
     * @param side the side
     * @return bids for {@link Side#BUY}, asks for {@link Side#SELL}
     */
    private TreeMap<Long, ArrayDeque<Order>> bookFor(Side side) {
        return side == Side.BUY ? bids : asks;
    }

    /**
     * Checks the fields common to every new order.
     *
     * @param id       the order id
     * @param quantity the order quantity
     * @throws IllegalArgumentException if quantity is not positive or the id is already on the book
     */
    private void validate(long id, long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (live.containsKey(id)) throw new IllegalArgumentException("duplicate order id " + id);
    }
}
