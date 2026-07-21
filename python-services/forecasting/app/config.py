import os
from typing import Literal

ForecastBackend = Literal["timesfm", "chronos", "statistical-fallback"]


def database_url() -> str:
    return os.getenv(
        "DATABASE_URL",
        "postgresql://tradernet:tradernet@postgres:5432/tradernet",
    )


def configured_backend() -> ForecastBackend:
    value = os.getenv("FORECAST_BACKEND", "statistical-fallback").strip().lower()
    if value == "timesfm":
        return "timesfm"
    if value == "chronos":
        return "chronos"
    return "statistical-fallback"
