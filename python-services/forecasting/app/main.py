from typing import Annotated

from fastapi import Depends, FastAPI, Query

from app.config import configured_backend
from app.forecast_service import ForecastService
from app.market_data import BinanceMarketDataClient
from app.models import ForecastResponse
from app.repositories import TimescalePriceHistoryRepository

app = FastAPI(title="Tradernet Forecasting Service", version="1.0.0")

_forecast_service = ForecastService(
    price_history_repository=TimescalePriceHistoryRepository(),
    fallback_market_data=BinanceMarketDataClient(),
)


def get_forecast_service() -> ForecastService:
    return _forecast_service


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "backend": configured_backend()}


@app.get("/forecast", response_model=ForecastResponse)
def forecast(
    service: Annotated[ForecastService, Depends(get_forecast_service)],
    symbol: str = Query(default="BTCUSDT", min_length=3, max_length=32),
    horizon_days: int = Query(default=30, ge=1, le=365),
) -> ForecastResponse:
    return service.forecast(symbol, horizon_days)
