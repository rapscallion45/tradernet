import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"

const refetchIntervalMs = 1_000

/**
 * Fetches backend-maintained Binance L2 depth for the selected chart symbol.
 */
export const useMarketOrderBook = (selectedSymbol: string, currency: string, levels = 12) => {
  const symbol = selectedSymbol || DEFAULT_CHART_SYMBOL

  return useQuery({
    queryKey: [QueryClientKeys.MarketOrderBook, symbol, currency, levels],
    queryFn: () => getRestClient().marketResource.getOrderBook(symbol, levels, currency),
    refetchInterval: refetchIntervalMs,
  })
}
