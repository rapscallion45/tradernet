import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"

const refetchIntervalMs = 5 * 60_000
const defaultHorizonDays = 30

/**
 * Fetches the current forecast narrative and bull score for the selected chart symbol.
 */
export const useMarketForecast = (selectedSymbol: string, horizonDays = defaultHorizonDays) => {
  const symbol = selectedSymbol || DEFAULT_CHART_SYMBOL

  return useQuery({
    queryKey: [QueryClientKeys.MarketForecast, symbol, horizonDays],
    queryFn: () => getRestClient().marketResource.getForecast(symbol, horizonDays),
    refetchInterval: refetchIntervalMs,
  })
}
