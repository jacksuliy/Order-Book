# Order Book & Matching Engine (Java)

A single-instrument limit order book with price-time priority matching.

## Features
- Limit and market orders, cancels
- Price-time priority: best price first, then earliest order at that price
- Trades execute at the resting order's price
- Prices stored as `long` ticks to avoid floating-point error

## Design
| Component | Structure | Why |
|---|---|---|
| Bid side | `TreeMap<Long, ArrayDeque<Order>>`, descending | O(log n) best-price access, sorted levels |
| Ask side | `TreeMap<Long, ArrayDeque<Order>>`, ascending | same |
| Price level | `ArrayDeque` (FIFO) | time priority |
| Order lookup | `HashMap<Long, Order>` | O(1) find for cancel |

## Build & test
```
mvn test
```

## Roadmap
- [ ] Replay historical market data
- [ ] JMH benchmarks (throughput, p50/p99 latency)
- [ ] O(1) cancel via intrusive doubly linked list per level
- [ ] Array-indexed price ladder vs. TreeMap comparison
- [ ] Backtester with a simple market-making strategy (PnL, Sharpe, drawdown)
