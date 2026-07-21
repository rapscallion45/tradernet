import logging
from typing import Protocol

import psycopg

from app.config import database_url

LOG = logging.getLogger(__name__)


class PriceHistoryRepository(Protocol):
    def load_closes(self, symbol: str, limit: int) -> list[float]:
        """Load recent closes in chronological order."""


class TimescalePriceHistoryRepository:
    _LOAD_CLOSES_SQL = """
        SELECT close
          FROM market_bars
         WHERE symbol = %s
         ORDER BY bucket DESC
         LIMIT %s
    """

    def load_closes(self, symbol: str, limit: int) -> list[float]:
        try:
            with psycopg.connect(database_url(), connect_timeout=3) as connection:
                with connection.cursor() as cursor:
                    cursor.execute(self._LOAD_CLOSES_SQL, (symbol.upper(), limit))
                    rows = cursor.fetchall()
        except psycopg.Error as exc:
            LOG.warning("Could not load market history for %s from TimescaleDB", symbol, exc_info=exc)
            return []

        closes = [float(row[0]) for row in rows if row and row[0] is not None and float(row[0]) > 0]
        closes.reverse()
        return closes
