import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, DEFAULT_FORECAST_HORIZON_DAYS, QueryClientKeys } from "global/constants"

const refetchIntervalMs = 5 * 60_000

/**
 * Fetches the current forecast narrative and bull score for the selected chart symbol.
 */
export const useMarketForecast = (selectedSymbol: string, horizonDays = DEFAULT_FORECAST_HORIZON_DAYS) => {
  const symbol = selectedSymbol || DEFAULT_CHART_SYMBOL

  return useQuery({
    queryKey: [QueryClientKeys.MarketForecast, symbol, horizonDays],
    queryFn: () => getRestClient().marketResource.getForecast(symbol, horizonDays),
    refetchInterval: refetchIntervalMs,
  })
}
