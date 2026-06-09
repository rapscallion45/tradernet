import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"

const refetchIntervalMs = 60_000

/**
 * Fetches normalized market context scores for the currently selected chart symbol.
 */
export const useMarketContext = (selectedSymbol: string) => {
  const symbol = selectedSymbol || DEFAULT_CHART_SYMBOL

  return useQuery({
    queryKey: [QueryClientKeys.MarketContext, symbol],
    queryFn: () => getRestClient().marketResource.getMarketContext(symbol),
    refetchInterval: refetchIntervalMs,
  })
}
