import { useMemo } from "react"
import { useQueries } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"
import { getSymbolMetrics } from "hooks/useSymbolMetrics"

const refetchIntervalMs = 15_000

/**
 * Fetches watchlist bars and returns symbol metrics for each symbol.
 */
export const useWatchlistSymbolMetrics = (symbols: string[], currency: string) => {
  const watchlistQueries = useQueries({
    queries: symbols.map((symbol) => ({
      queryKey: [QueryClientKeys.MarketBars, symbol, currency, "charts-watchlist"],
      queryFn: () => getRestClient().marketResource.getBars(symbol, "1M", 2, currency),
      refetchInterval: refetchIntervalMs,
    })),
  })

  return useMemo(
    () =>
      symbols.map((symbol, index) => {
        const bars = watchlistQueries[index]?.data ?? []
        const metrics = getSymbolMetrics(bars)
        return { symbol, ...metrics }
      }),
    [symbols, watchlistQueries]
  )
}
