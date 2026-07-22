# Market Signal Accuracy Architecture

Tradernet's market AI service now supports a context-aware signal path that can blend short-term technical signals with normalized symbol-specific market intelligence.

## Signal flow

1. Scheduled ingestion jobs collect provider-specific data outside the real-time stream path:
   - ETF or fund flows when available for the selected asset.
   - On-chain exchange net flows and valuation metrics when the selected asset supports them.
   - Derivatives funding and open interest from providers such as Coinglass, Deribit, or Binance Futures.
   - Macro liquidity data from FRED/BLS sources.
   - Sentiment data such as fear-and-greed or search interest.
2. Each ingestion job writes normalized features as z-scores or bounded directional scores.
3. `MarketContextRegistry` stores the latest normalized context by symbol and enriches the technical feature snapshot.
4. `MarketRegimeScoreEngine` converts technical, ETF, on-chain, derivatives, valuation, macro, and sentiment features into a 0-100 score.
5. `ContextAwareSignalScorer` blends the broader market regime score with the cached forecast bull score, emits buy/sell signals when that effective context agrees with the technical model, emits strong directional signals when the effective score reaches an extreme, and emits `HOLD` when the combined evidence is neutral or contradictory.

## Built-in Java ingestion

`MarketContextService` schedules in-app market context refreshes every 15 minutes. It hydrates symbols listed in `market.ai.context.symbols`, the boot-time default `market.ai.symbol`, any symbol requested through `GET /market/context`, and any symbol acquired by `LiveMarketPipelineService` for a chart websocket.

The no-key default ingestion currently fetches:

- Binance USD-M Futures funding-rate history for `fundingRateZScore`.
- Binance USD-M Futures open-interest history for `openInterestChangeZScore`.
- Alternative.me Fear & Greed data for `sentimentZScore`.

Administrators can update richer context through `POST /market/context` for ETF/fund flows, exchange outflows, MVRV valuation, and macro liquidity. Standard users have read-only market access. The write payload accepts raw normalized z-score inputs only; the backend response includes calculated bullish-percent fields and per-input availability flags for UI display so clients do not duplicate scoring formulas or show fallback percentages when data is missing.

Runtime switches:

- `-Dmarket.ai.context.ingestion.enabled=false` disables the scheduled Java ingestion.
- `-Dmarket.ai.context.symbols=BTCUSDT,ETHUSDT` preloads additional symbols for scheduled refresh.

## Score interpretation

- `0-24`: Bearish regime.
- `25-49`: Weak or uncertain regime.
- `50-74`: Bullish regime.
- `75-100`: Strong but potentially overheated regime.

## Real-time signal confidence

The WebSocket signal payload carries two related but separate ideas:

- `side`: `BUY`, `SELL`, or `HOLD`. The backend now emits `HOLD` as a first-class signal when the scorer is inside the neutral band or when technical and context inputs conflict.
- `confidence`: conviction in that side, not a raw buy probability. Directional `BUY`/`SELL` confidence is widened from threshold-level weak signals toward high-conviction signals as the technical probability and market-regime score move farther from neutral. `HOLD` confidence is highest when the evidence is close to neutral and drops as the market becomes more directional.

This lets the UI render direction and strength independently, for example `BUY` + `Weak`, `HOLD` + `Strong`, or `SELL` + `Medium`, instead of showing every threshold-crossing signal as roughly 60-65%.

The chart signal is intentionally short-term. The technical model creates the first BUY/SELL/HOLD decision from EMA/RSI features; market context and the cached forecast bull score then confirm, block, or only at extremes promote that decision. A high bull score pulls the effective context toward BUY, a low bull score pulls it toward SELL, and a bull score near 50 triggers a neutral forecast filter that biases directional technical votes back to HOLD.

The Market Score Inputs card displays backend-calculated values from the same context family in a friendlier 0-100 bullish-tilt scale: 50% is neutral only when input data exists, higher values support bullish context, and lower values support bearish context. Missing inputs display `No data` in the UI rather than a fallback 50% value. Internally the backend still stores normalized z-score/directional inputs, converts available inputs into a 0-100 `market_score`, blends that score with the forecast bull score into `effective_context_score`, and then combines it with the short-term EMA/RSI technical decision.

A real backend `HOLD` is different from the frontend `No signal` fallback. `No signal` means no `AiSignal` has been received for the selected chart symbol yet; once a signal arrives, the chart displays the backend side and appends the latest signal model version plus prioritized notes to the legend for debugging and operator context. Opening a chart websocket dynamically starts a live Binance trade stream for the selected symbol, and each live symbol has its own bar aggregator, feature engine, and signal engine. Until the first live signal arrives, `GET /api/market/signals` can still generate an initial signal from recent Binance klines so non-BTC charts do not remain blank.

## Runtime configuration

The default scorer is now context-aware. Existing scorers are still available:

- `-Dmarket.ai.scorer=context` for context-aware scoring.
- `-Dmarket.ai.scorer=linear` for the previous lightweight logistic model.
- `-Dmarket.ai.scorer=rules` for the original EMA/RSI threshold rules.

## Forecasting and Gemma 4 narrative layer

Tradernet also exposes a cache-first forecast path through `GET /api/market/forecast?symbol=BTCUSDT&horizonDays=1` for the default daily-trading view; callers can still request longer horizons with `horizonDays`. REST requests receive the latest immutable snapshot immediately, or a deterministic unavailable snapshot while the cache is cold. Expired entries remain readable while one asynchronous EJB refresh runs.

1. `MarketForecastService` coalesces refresh requests per symbol and horizon and hydrates the same market context used by the real-time signal scorer in the background.
2. `ForecastingClient` calls the Python forecasting service (`market.ai.forecasting.url`, default `http://forecasting-service:8000`) from that managed asynchronous boundary.
3. The Python service reads recent bars from Postgres/TimescaleDB, falls back to recent Binance 1-minute klines when the selected symbol has insufficient TimescaleDB history, and returns a probability of positive return, expected return, bull score, model name, and driver labels that Java adapts into structured API driver objects. If both data sources are insufficient, it returns a neutral forecast rather than a hardcoded bullish score. It ships with a statistical fallback and stable adapter hooks for `FORECAST_BACKEND=timesfm` or `FORECAST_BACKEND=chronos` custom images.
4. `OllamaNarrativeClient` sends the structured forecast to Ollama (`market.ai.ollama.url`, default `http://ollama:11434`) using Gemma 4 (`market.ai.ollama.model`, default `gemma4:e4b`).
5. If Ollama or the Python service is unavailable, the refresh stores a deterministic fallback so the UI can continue rendering without provider latency on the request thread. Java also normalizes Ollama text back to the selected symbol if the model emits hardcoded Bitcoin wording.

Example narrative shape:

> Today's BTCUSDT Bull Score is 74. ETF inflows remain positive, exchange balances continue declining, and funding rates remain neutral. Probability of a positive 1-day return: 64%.

Runtime switches:

- `-Dmarket.ai.model.buyThreshold=0.56` controls how easily the technical model emits BUY.
- `-Dmarket.ai.model.sellThreshold=0.44` controls how easily the technical model emits SELL.
- `-Dmarket.ai.context.buyScoreThreshold=54` confirms/blocks BUY with market context.
- `-Dmarket.ai.context.sellScoreThreshold=46` confirms/blocks SELL with market context.
- `-Dmarket.ai.context.buyExtremeThreshold=64` allows strong bullish context to promote HOLD to BUY.
- `-Dmarket.ai.context.sellExtremeThreshold=36` allows strong bearish effective context to promote HOLD to SELL.
- `-Dmarket.ai.context.forecastWeight=0.45` controls how strongly forecast bull score influences chart signal context.
- `-Dmarket.ai.context.forecastNeutralBand=8` treats bull scores from 42 to 58 as neutral and biases signals to HOLD.
- `-Dmarket.ai.signalBullScore.enabled=true` enables bull-score enrichment for chart signals.
- `-Dmarket.ai.signalBullScoreHorizonDays=1` chooses the forecast horizon used by chart signals.
- `-Dmarket.ai.signalBullScoreTtlMs=60000` caches bull-score lookups for one minute.
- `-Dmarket.ai.forecast.ttlMs=60000` controls the stale-while-revalidate forecast snapshot TTL.
- `-Dmarket.ai.forecasting.url=http://forecasting-service:8000` points Java at the Python service.
- `-Dmarket.ai.ollama.enabled=false` disables LLM narratives and uses deterministic text.
- `-Dmarket.ai.ollama.url=http://ollama:11434` points Java at Ollama.
- `-Dmarket.ai.ollama.model=gemma4:e4b` selects the local Gemma 4 tag.
