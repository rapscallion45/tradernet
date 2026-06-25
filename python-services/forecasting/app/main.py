import json
import math
import os
from datetime import datetime, timezone
from urllib.parse import urlencode
from urllib.request import urlopen
from typing import Literal

import numpy as np
import psycopg
from fastapi import FastAPI, Query
from pydantic import BaseModel, Field

ForecastBackend = Literal["timesfm", "chronos", "statistical-fallback"]

app = FastAPI(title="Tradernet Forecasting Service", version="1.0.0")


class ForecastResponse(BaseModel):
    symbol: str
    horizon_days: int = Field(serialization_alias="horizon_days")
    probability_positive_return: float
    expected_return: float
    bull_score: float
    model: str
    drivers: list[str]
    generated_at: datetime


def database_url() -> str:
    return os.getenv(
        "DATABASE_URL",
        "postgresql://tradernet:tradernet@postgres:5432/tradernet",
    )


def configured_backend() -> ForecastBackend:
    value = os.getenv("FORECAST_BACKEND", "statistical-fallback").strip().lower()
    if value in {"timesfm", "chronos"}:
        return value  # type: ignore[return-value]
    return "statistical-fallback"


def load_timescale_closes(symbol: str, limit: int = 720) -> list[float]:
    query = """
        SELECT close
          FROM market_bars
         WHERE symbol = %s
         ORDER BY bucket DESC
         LIMIT %s
    """
    try:
        with psycopg.connect(database_url(), connect_timeout=3) as conn:
            with conn.cursor() as cur:
                cur.execute(query, (symbol.upper(), limit))
                rows = cur.fetchall()
    except Exception:
        return []

    closes = [float(row[0]) for row in rows if row and row[0] is not None and float(row[0]) > 0]
    closes.reverse()
    return closes


def load_binance_closes(symbol: str, limit: int = 720) -> list[float]:
    query = urlencode({"symbol": symbol.upper(), "interval": "1m", "limit": max(3, min(limit, 1000))})
    url = f"https://api.binance.com/api/v3/klines?{query}"
    try:
        with urlopen(url, timeout=5) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except Exception:
        return []

    closes: list[float] = []
    for row in payload:
        if not isinstance(row, list) or len(row) < 5:
            continue
        try:
            close = float(row[4])
        except (TypeError, ValueError):
            continue
        if close > 0:
            closes.append(close)
    return closes


def load_recent_closes(symbol: str, limit: int = 720) -> tuple[list[float], str]:
    timescale_closes = load_timescale_closes(symbol, limit)
    if len(timescale_closes) >= 3:
        return timescale_closes, "timescaledb"

    binance_closes = load_binance_closes(symbol, limit)
    if len(binance_closes) >= 3:
        return binance_closes, "binance-klines"

    return timescale_closes, "insufficient-history"


def statistical_forecast(closes: list[float], horizon_days: int, source: str) -> tuple[float, float, list[str]]:
    if len(closes) < 3:
        return 0.5, 0.0, ["insufficient price history", "waiting for TimescaleDB or Binance klines"]

    prices = np.array(closes, dtype=float)
    returns = np.diff(np.log(prices))
    if len(returns) == 0:
        return 0.5, 0.0, ["flat price history"]

    recent_window = returns[-min(len(returns), 90):]
    momentum = float(np.mean(recent_window))
    volatility = float(np.std(recent_window)) or 0.01
    expected_return = float(math.exp(momentum * horizon_days) - 1.0)
    z_score = expected_return / max(volatility * math.sqrt(horizon_days), 0.0001)
    probability_positive = 1.0 / (1.0 + math.exp(-z_score))
    probability_positive = float(np.clip(probability_positive, 0.05, 0.95))
    drivers = [
        f"price history source: {source}",
        "recent price momentum positive" if momentum >= 0 else "recent price momentum negative",
        "realized volatility elevated" if volatility > 0.025 else "realized volatility contained",
    ]
    return probability_positive, expected_return, drivers


def run_model_forecast(symbol: str, horizon_days: int) -> ForecastResponse:
    closes, source = load_recent_closes(symbol)
    backend = configured_backend()

    # The service is intentionally adapter-shaped: production images can install TimesFM or Chronos
    # dependencies and replace this section with the model call while keeping the HTTP contract stable.
    probability_positive, expected_return, drivers = statistical_forecast(closes, horizon_days, source)
    if backend == "timesfm":
        drivers.insert(0, "TimesFM adapter configured")
    elif backend == "chronos":
        drivers.insert(0, "Chronos adapter configured")

    bull_score = float(np.clip(50.0 + (probability_positive - 0.5) * 100.0 + expected_return * 100.0, 0.0, 100.0))
    return ForecastResponse(
        symbol=symbol.upper(),
        horizon_days=horizon_days,
        probability_positive_return=probability_positive,
        expected_return=expected_return,
        bull_score=bull_score,
        model=backend,
        drivers=drivers,
        generated_at=datetime.now(timezone.utc),
    )


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "backend": configured_backend()}


@app.get("/forecast", response_model=ForecastResponse)
def forecast(
    symbol: str = Query(default="BTCUSDT", min_length=3, max_length=32),
    horizon_days: int = Query(default=30, ge=1, le=365),
) -> ForecastResponse:
    return run_model_forecast(symbol, horizon_days)
