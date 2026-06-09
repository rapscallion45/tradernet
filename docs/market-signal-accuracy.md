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
5. `ContextAwareSignalScorer` emits buy/sell signals only when the technical model and the broader market regime score agree, or when the broader score reaches an extreme.

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

## Runtime configuration

The default scorer is now context-aware. Existing scorers are still available:

- `-Dmarket.ai.scorer=context` for context-aware scoring.
- `-Dmarket.ai.scorer=linear` for the previous lightweight logistic model.
- `-Dmarket.ai.scorer=rules` for the original EMA/RSI threshold rules.
