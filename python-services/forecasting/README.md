# Tradernet Forecasting Service

FastAPI service that exposes a stable forecasting contract for the Java market AI module.

- `FORECAST_BACKEND=timesfm` — use a custom image with TimesFM installed.
- `FORECAST_BACKEND=chronos` — use a custom image with Chronos installed.
- Default: `statistical-fallback`, which uses recent TimescaleDB bars so local Docker Compose works without GPU-only dependencies.

The Java service calls `GET /forecast?symbol=BTCUSDT&horizon_days=1` and sends the result to Ollama/Gemma 4 for a concise narrative.

When `market_bars` has fewer than three closes for a requested symbol, the service falls back to recent Binance 1-minute klines. If both TimescaleDB and Binance history are unavailable, it returns a neutral forecast (`bull_score=50`, `probability_positive_return=0.5`) rather than a hardcoded bullish value.
