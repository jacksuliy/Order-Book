package engine;

/**
 * An execution between an incoming (aggressor) order and an order resting on the book.
 * Trades always execute at the resting order's price.
 *
 * @param aggressorId   id of the incoming order that triggered the trade
 * @param restingId     id of the order that was already on the book
 * @param aggressorSide side of the incoming order
 * @param price         execution price, in ticks
 * @param quantity      number of units traded
 */
public record Trade(long aggressorId, long restingId, Side aggressorSide, long price, long quantity) { }
