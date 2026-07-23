import json
import logging
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import urlopen

LOG = logging.getLogger(__name__)


class BinanceMarketDataClient:
    def load_closes(self, symbol: str, limit: int) -> list[float]:
        query = urlencode({"symbol": symbol.upper(), "interval": "1m", "limit": max(3, min(limit, 1000))})
        url = f"https://api.binance.com/api/v3/klines?{query}"
        try:
            with urlopen(url, timeout=5) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except (HTTPError, URLError, TimeoutError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            LOG.warning("Could not load fallback market history for %s from Binance", symbol, exc_info=exc)
            return []

        closes: list[float] = []
        if not isinstance(payload, list):
            return closes

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
