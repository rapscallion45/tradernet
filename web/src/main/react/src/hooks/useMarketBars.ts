import { useMemo } from "react"
import { useQueries, useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { MarketBar } from "api/types"
import { QueryClientKeys } from "global/constants"

const refetchIntervalMs = 15_000

export type SymbolMetrics = {
  latest?: MarketBar
  delta: number
  deltaPercent: number
  dayHigh: number
  dayLow: number
  dayVolume: number
}

/**
 * Calculates 24-hour summary metrics for a symbol from market bars.
 */
export const getSymbolMetrics = (bars: MarketBar[]): SymbolMetrics => {
  const latest = bars[0]
  const reference = bars[bars.length - 1] ?? latest
  const delta = latest ? latest.close - reference.open : 0
  const deltaPercent = reference?.open ? (delta / reference.open) * 100 : 0

  return {
    latest,
    delta,
    deltaPercent,
    dayHigh: bars.length > 0 ? Math.max(...bars.map((bar) => bar.high)) : 0,
    dayLow: bars.length > 0 ? Math.min(...bars.map((bar) => bar.low)) : 0,
    dayVolume: bars.reduce((acc, bar) => acc + bar.volume, 0),
  }
}

/**
 * Fetches chart detail bars for the currently selected symbol.
 */
export const useChartDetailBars = (selectedSymbol: string, currency: string) => {
  return useQuery({
    queryKey: [QueryClientKeys.MarketBars, selectedSymbol, currency, "charts-detail"],
    queryFn: () => getRestClient().marketResource.getBars(selectedSymbol, "1M", 24, currency),
    refetchInterval: refetchIntervalMs,
  })
}

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
