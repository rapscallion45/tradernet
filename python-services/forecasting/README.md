# Tradernet Forecasting Service

FastAPI service that exposes a stable forecasting contract for the Java market AI module.

- `FORECAST_BACKEND=timesfm` — use a custom image with TimesFM installed.
- `FORECAST_BACKEND=chronos` — use a custom image with Chronos installed.
- Default: `statistical-fallback`, which uses recent TimescaleDB bars so local Docker Compose works without GPU-only dependencies.

The Java service calls `GET /forecast?symbol=BTCUSDT&horizon_days=30` and sends the result to Ollama/Gemma 4 for a concise narrative.
