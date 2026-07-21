from datetime import datetime

from pydantic import BaseModel, Field


class ForecastResponse(BaseModel):
    symbol: str
    horizon_days: int = Field(serialization_alias="horizon_days")
    probability_positive_return: float
    expected_return: float
    bull_score: float
    model: str
    drivers: list[str]
    generated_at: datetime
