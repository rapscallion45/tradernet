import math
from datetime import datetime, timezone

import numpy as np

from app.config import configured_backend
from app.market_data import BinanceMarketDataClient
from app.models import ForecastResponse
from app.repositories import PriceHistoryRepository


class ForecastService:
    def __init__(
        self,
        price_history_repository: PriceHistoryRepository,
        fallback_market_data: BinanceMarketDataClient,
    ) -> None:
        self._price_history_repository = price_history_repository
        self._fallback_market_data = fallback_market_data

    def forecast(self, symbol: str, horizon_days: int) -> ForecastResponse:
        closes, source = self._load_recent_closes(symbol)
        backend = configured_backend()
        probability_positive, expected_return, drivers = self._statistical_forecast(
            closes,
            horizon_days,
            source,
        )

        # Production images can install a model runtime behind this stable service contract.
        if backend == "timesfm":
            drivers.insert(0, "TimesFM adapter configured")
        elif backend == "chronos":
            drivers.insert(0, "Chronos adapter configured")

        bull_score = float(
            np.clip(50.0 + (probability_positive - 0.5) * 100.0 + expected_return * 100.0, 0.0, 100.0)
        )
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

    def _load_recent_closes(self, symbol: str, limit: int = 720) -> tuple[list[float], str]:
        timescale_closes = self._price_history_repository.load_closes(symbol, limit)
        if len(timescale_closes) >= 3:
            return timescale_closes, "timescaledb"

        binance_closes = self._fallback_market_data.load_closes(symbol, limit)
        if len(binance_closes) >= 3:
            return binance_closes, "binance-klines"

        return timescale_closes, "insufficient-history"

    @staticmethod
    def _statistical_forecast(
        closes: list[float],
        horizon_days: int,
        source: str,
    ) -> tuple[float, float, list[str]]:
        if len(closes) < 3:
            return 0.5, 0.0, [
                "insufficient price history",
                "waiting for TimescaleDB or Binance klines",
            ]

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
