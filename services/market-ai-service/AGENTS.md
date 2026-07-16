# Market AI service agent guide

## Responsibility
`services/market-ai-service` owns live market ingestion, bar aggregation, technical/context features, display-ready market data views, AI signal generation, market bar persistence, forecasting service integration, and Ollama narrative integration.

## Key packages
- `engine`: market event publishing, feature generation, signal engine, chart intervals, and bar/tick handling.
- `scoring`: pluggable signal scorers (`context`, `linear`, `rules`) and confidence semantics.
- `context`: market context registry, ingestion client, and market regime scoring.
- `forecast`: Java HTTP bridge to the Python forecasting service, forecast DTO, and Ollama narrative client.
- `orderbook`: Binance order-book service, stream client, and snapshot DTOs.
- `model`: market bars, feature snapshots, signals, sides, trades, and context snapshots.

## Conventions
- Keep service-level collaborators EJB-managed; keep per-symbol runtime objects such as aggregators, feature engines, signal engines, trade-stream clients, and order-book clients as plain Java objects unless the container needs to own their lifecycle directly.
- Keep `HOLD` and frontend `No signal` semantics distinct: backend `HOLD` is an emitted `AiSignal`; frontend `No signal` means no signal payload was received yet.
- Keep market scoring, forecast blending, normalization, and display-ready derived market metrics in this backend module; the frontend should consume these values rather than reimplementing formulas.
- Forecasting should degrade gracefully when Python or Ollama is unavailable.
- Avoid adding hard dependencies on heavy ML runtimes inside Java; plug those into the Python forecasting adapter instead.
- When changing scoring thresholds or signal semantics, update `docs/market-signal-accuracy.md`.
- When changing forecast payloads or runtime properties, update `docs/application-guide.md` and frontend API types.
