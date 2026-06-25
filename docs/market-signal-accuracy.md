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
5. `ContextAwareSignalScorer` emits buy/sell signals when the technical model and the broader market regime score agree, emits strong directional signals when the broader score reaches an extreme, and emits `HOLD` when the combined evidence is neutral or contradictory.

## Built-in Java ingestion

`MarketAiService` now schedules in-app market context refreshes every 15 minutes. The scheduler hydrates symbols listed in `market.ai.context.symbols`, the active `market.ai.symbol`, and any symbol requested through `GET /market/context`.

The no-key default ingestion currently fetches:

- Binance USD-M Futures funding-rate history for `fundingRateZScore`.
- Binance USD-M Futures open-interest history for `openInterestChangeZScore`.
- Alternative.me Fear & Greed data for `sentimentZScore`.

Provider-backed ingestion can still update richer context through `POST /market/context` for ETF/fund flows, exchange outflows, MVRV valuation, and macro liquidity.

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

The chart signal is intentionally short-term. The technical model creates the first BUY/SELL/HOLD decision from EMA/RSI features; market context then confirms, blocks, or only at extremes promotes that decision. The forecast horizon is not directly weighted in the chart BUY/HOLD/SELL signal, so changing from a 30-day to 1-day forecast changes the forecast card/order bull score but not the realtime signal vote.

A real backend `HOLD` is different from the frontend `No signal` fallback. `No signal` means no `AiSignal` has been received for the selected chart symbol yet; once a signal arrives, the chart displays the backend side and appends the latest signal model version plus up to three notes to the legend for debugging and operator context.

## Runtime configuration

The default scorer is now context-aware. Existing scorers are still available:

- `-Dmarket.ai.scorer=context` for context-aware scoring.
- `-Dmarket.ai.scorer=linear` for the previous lightweight logistic model.
- `-Dmarket.ai.scorer=rules` for the original EMA/RSI threshold rules.

## Forecasting and Gemma 4 narrative layer

Tradernet also exposes a forecast path through `GET /api/market/forecast?symbol=BTCUSDT&horizonDays=1` for the default daily-trading view; callers can still request longer horizons with `horizonDays`.

1. The Java market AI service hydrates the same market context used by the real-time signal scorer.
2. `ForecastingClient` calls the Python forecasting service (`market.ai.forecasting.url`, default `http://forecasting-service:8000`).
3. The Python service reads recent bars from Postgres/TimescaleDB and returns a probability of positive return, expected return, bull score, model name, and drivers. It ships with a statistical fallback and stable adapter hooks for `FORECAST_BACKEND=timesfm` or `FORECAST_BACKEND=chronos` custom images.
4. `OllamaNarrativeClient` sends the structured forecast to Ollama (`market.ai.ollama.url`, default `http://ollama:11434`) using Gemma 4 (`market.ai.ollama.model`, default `gemma4:e4b`).
5. If Ollama or the Python service is unavailable, the API returns a deterministic fallback sentence so the UI can continue rendering.

Example narrative shape:

> Today's Bitcoin Bull Score is 74. ETF inflows remain positive, exchange balances continue declining, and funding rates remain neutral. Probability of a positive 1-day return: 64%.

Runtime switches:

- `-Dmarket.ai.model.buyThreshold=0.56` controls how easily the technical model emits BUY.
- `-Dmarket.ai.model.sellThreshold=0.44` controls how easily the technical model emits SELL.
- `-Dmarket.ai.context.buyScoreThreshold=54` confirms/blocks BUY with market context.
- `-Dmarket.ai.context.sellScoreThreshold=46` confirms/blocks SELL with market context.
- `-Dmarket.ai.context.buyExtremeThreshold=64` allows strong bullish context to promote HOLD to BUY.
- `-Dmarket.ai.context.sellExtremeThreshold=36` allows strong bearish context to promote HOLD to SELL.
- `-Dmarket.ai.forecasting.url=http://forecasting-service:8000` points Java at the Python service.
- `-Dmarket.ai.ollama.enabled=false` disables LLM narratives and uses deterministic text.
- `-Dmarket.ai.ollama.url=http://ollama:11434` points Java at Ollama.
- `-Dmarket.ai.ollama.model=gemma4:e4b` selects the local Gemma 4 tag.
