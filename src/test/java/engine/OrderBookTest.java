package engine;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    @Test void nonCrossingOrdersRest() {
        OrderBook b = new OrderBook();
        assertTrue(b.submitLimit(1, Side.BUY, 100, 10).isEmpty());
        assertTrue(b.submitLimit(2, Side.SELL, 101, 10).isEmpty());
        assertEquals(100, b.bestBid().getAsLong());
        assertEquals(101, b.bestAsk().getAsLong());
    }

    @Test void fullFillRemovesBothOrders() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 100, 10);
        List<Trade> t = b.submitLimit(2, Side.BUY, 100, 10);
        assertEquals(List.of(new Trade(2, 1, Side.BUY, 100, 10)), t);
        assertTrue(b.bestAsk().isEmpty());
        assertTrue(b.bestBid().isEmpty());
    }

    @Test void partialFillRestsRemainder() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 100, 4);
        b.submitLimit(2, Side.BUY, 100, 10);
        assertEquals(6, b.depthAt(Side.BUY, 100));
        assertTrue(b.isResting(2));
        assertFalse(b.isResting(1));
    }

    @Test void tradesAtRestingPrice() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 100, 5);
        List<Trade> t = b.submitLimit(2, Side.BUY, 105, 5);
        assertEquals(100, t.get(0).price());
    }

    @Test void pricePriority() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 102, 5);
        b.submitLimit(2, Side.SELL, 101, 5);
        List<Trade> t = b.submitLimit(3, Side.BUY, 102, 5);
        assertEquals(2, t.get(0).restingId());
    }

    @Test void timePriorityWithinLevel() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 100, 5);
        b.submitLimit(2, Side.SELL, 100, 5);
        List<Trade> t = b.submitLimit(3, Side.BUY, 100, 7);
        assertEquals(1, t.get(0).restingId());
        assertEquals(5, t.get(0).quantity());
        assertEquals(2, t.get(1).restingId());
        assertEquals(2, t.get(1).quantity());
        assertEquals(3, b.depthAt(Side.SELL, 100));
    }

    @Test void sweepsMultipleLevels() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.BUY, 100, 5);
        b.submitLimit(2, Side.BUY, 99, 5);
        b.submitLimit(3, Side.BUY, 98, 5);
        List<Trade> t = b.submitLimit(4, Side.SELL, 99, 12);
        assertEquals(2, t.size());
        assertEquals(98, b.bestBid().getAsLong());
        assertEquals(2, b.depthAt(Side.SELL, 99));
    }

    @Test void marketOrderNeverRests() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 100, 3);
        List<Trade> t = b.submitMarket(2, Side.BUY, 10);
        assertEquals(3, t.get(0).quantity());
        assertFalse(b.isResting(2));
        assertTrue(b.bestBid().isEmpty());
    }

    @Test void cancelRemovesOrderAndEmptyLevel() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.BUY, 100, 5);
        assertTrue(b.cancel(1));
        assertFalse(b.cancel(1));
        assertTrue(b.bestBid().isEmpty());
    }

    @Test void cancelledOrderDoesNotTrade() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.SELL, 100, 5);
        b.submitLimit(2, Side.SELL, 100, 5);
        b.cancel(1);
        List<Trade> t = b.submitLimit(3, Side.BUY, 100, 5);
        assertEquals(2, t.get(0).restingId());
    }

    @Test void rejectsBadInput() {
        OrderBook b = new OrderBook();
        b.submitLimit(1, Side.BUY, 100, 5);
        assertThrows(IllegalArgumentException.class, () -> b.submitLimit(1, Side.BUY, 100, 5));
        assertThrows(IllegalArgumentException.class, () -> b.submitLimit(2, Side.BUY, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> b.submitLimit(3, Side.BUY, -1, 5));
    }
}
