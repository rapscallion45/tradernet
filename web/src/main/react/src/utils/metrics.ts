import { MarketBar } from "api/types"

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
