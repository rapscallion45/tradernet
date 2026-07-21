# Python forecasting service agent guide

## Responsibility
`python-services/forecasting` is a FastAPI adapter used by Java market AI to produce forecast probabilities, expected returns, bull scores, model names, and drivers.

## Key files
- `app/main.py`: thin FastAPI route and dependency wiring.
- `app/forecast_service.py`: forecast orchestration and statistical fallback.
- `app/repositories.py`: TimescaleDB price-history repository.
- `app/market_data.py`: Binance fallback gateway.
- `app/models.py`: HTTP response model.
- `app/config.py`: environment-backed runtime configuration.
- `Dockerfile`: container image for the forecasting service.
- `requirements.txt`: Python dependencies for the lightweight fallback service.
- `README.md`: service contract and backend selector notes.

## Conventions
- Keep the `/forecast` HTTP contract stable for `ForecastingClient`.
- Preserve `route -> service -> repository -> database` layering. Routes must not issue SQL or call database drivers directly; external provider calls belong in gateway modules rather than repositories.
- Keep `statistical-fallback` lightweight and runnable without GPU/model dependencies.
- Add TimesFM/Chronos support through custom images or optional adapter code, not by making the base local image heavy.
- Use `DATABASE_URL` for Postgres/TimescaleDB access and `FORECAST_BACKEND` to choose adapter behavior.
- If the response shape changes, update Java `ForecastingClient`, TypeScript `MarketForecast`, and docs.

## Useful checks
- `python3 -m py_compile app/*.py`
