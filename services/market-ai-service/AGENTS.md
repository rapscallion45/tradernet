# Market AI service agent guide

## Responsibility
`services/market-ai-service` owns live market ingestion, bar aggregation, technical/context features, display-ready market data views, AI signal generation, market bar persistence, forecasting service integration, and Ollama narrative integration.

## Key packages
- `engine`: market event publishing, feature generation, signal engine, chart intervals, and bar/tick handling.
- `scoring`: pluggable signal scorers (`context`, `linear`, `rules`) and confidence semantics.
- `context`: market context registry, ingestion client, and market regime scoring.
- `forecast`: cache-first forecast application service, forecast DTO, and HTTP bridges to Python forecasting and Ollama.
- `orderbook`: Binance lifecycle/reconciliation client, provider payload parser, snapshot factory, and snapshot DTOs.
- `model`: market bars, feature snapshots, signals, sides, trades, and context snapshots.

## Conventions
- Keep service-level collaborators EJB-managed; keep per-symbol runtime objects such as aggregators, feature engines, signal engines, trade-stream clients, and order-book clients as plain Java objects unless the container needs to own their lifecycle directly.
- Publish focused bar, signal, symbol, forecast, context, order-book, and live-pipeline services. Do not recreate a single market facade that forwards all operations.
- Keep slow IO outside singleton write locks and request/websocket hot paths. Use managed async EJB methods and cached snapshots for Binance websocket startup, order-book resync, forecast bull-score refreshes, and closed-bar persistence.
- Market context and forecast reads must return cached snapshots immediately and single-flight asynchronous refreshes. Do not wait for context providers, Python forecasting, and Ollama sequentially on a REST request.
- Persist closed bars through `MarketBarStorageService -> MarketBarDao`; SQL and datasource access belong to the DAO implementation in `data-model`, never in this service module.
- Route trade-stream reconnects and order-book gap resync through intercepted asynchronous EJB methods. A plain Java websocket callback must only update local state and request managed recovery; it must not fetch a REST snapshot itself.
- Bound live trade-stream runtimes by configured symbol capacity and release non-default streams when their last subscriber disconnects. Bound order-book clients separately and evict them after configured idle time.
- Isolate in-process market event subscribers. One failing bar/signal listener must not break publishing to other listeners or interrupt ingestion callbacks.
- Keep API websocket subscribers lightweight: filter and enqueue only. Currency conversion, serialization, and client sends belong behind bounded per-session delivery queues outside the ingestion callback.
- Java `HttpClient` websocket text callbacks may deliver one JSON message across multiple `onText` fragments. Accumulate text until `last == true` before parsing market stream payloads.
- Keep order-book provider parsing in `BinanceOrderBookPayloadParser` and public snapshot calculations in `OrderBookSnapshotFactory`; the live client owns connection lifecycle, update sequencing, resync, and mutable book state.
- Centralize market properties in `MarketAiConfiguration`, validate them once, and pass immutable `SignalScoringSettings` to per-symbol plain Java engines.
- Keep `HOLD` and frontend `No signal` semantics distinct: backend `HOLD` is an emitted `AiSignal`; frontend `No signal` means no signal payload was received yet.
- Preserve explicit availability flags for every market-context input. Zero is a valid neutral z-score and must remain distinguishable from missing provider data across copies and partial updates.
- Keep market scoring, forecast blending, normalization, and display-ready derived market metrics in this backend module; the frontend should consume these values rather than reimplementing formulas.
- Use `domain-model` for canonical market-symbol normalization shared with API, order, and trade modules.
- Forecasting should degrade gracefully when Python or Ollama is unavailable.
- Avoid adding hard dependencies on heavy ML runtimes inside Java; plug those into the Python forecasting adapter instead.
- When changing scoring thresholds or signal semantics, update `docs/market-signal-accuracy.md`.
- When changing forecast payloads or runtime properties, update `docs/application-guide.md` and frontend API types.
