import { FC, useMemo, useState } from "react"
import { Grid, Stack } from "@mantine/core"
import { useViewportSize } from "@mantine/hooks"
import { TradingChartPanel } from "components/TradingChartPanel/TradingChartPanel"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"
import { useChartDetailBars } from "hooks/useChartDetailBars"
import { getSymbolMetrics } from "hooks/useSymbolMetrics"
import { useWatchlistSymbolMetrics } from "hooks/useWatchlistSymbolMetrics"
import { useMarketSymbols } from "hooks/useMarketSymbols"
import { ChartDetailCard } from "pages/Charts/components/ChartDetailCard"
import { ChartsWatchlistCard } from "pages/Charts/components/ChartsWatchlistCard"

const watchlistLimit = 8

/**
 * Dedicated chart-focused page with market details and watchlist.
 */
const ChartsPage: FC = () => {
  const { currency } = useCurrencyPreference()
  const { height: viewportHeight } = useViewportSize()
  const [selectedSymbol, setSelectedSymbol] = useState("BTCUSDT")
  const { data: symbolOptions = [] } = useMarketSymbols()
  const chartHeight = Math.max(520, viewportHeight - 180)

  const watchlistSymbols = useMemo(() => {
    const symbols = symbolOptions.length > 0 ? symbolOptions.slice(0, watchlistLimit) : [selectedSymbol]
    return symbols.includes(selectedSymbol) ? symbols : [selectedSymbol, ...symbols].slice(0, watchlistLimit)
  }, [selectedSymbol, symbolOptions])

  const { data: selectedBars = [], isLoading: isSelectedBarsLoading } = useChartDetailBars(selectedSymbol, currency)
  const watchlistRows = useWatchlistSymbolMetrics(watchlistSymbols, currency)
  const detailMetrics = useMemo(() => getSymbolMetrics(selectedBars), [selectedBars])

  return (
    <Stack gap="md" h={`calc(100dvh - 120px)`}>
      <Grid gutter="md" align="stretch" style={{ flex: 1 }}>
        <Grid.Col span={{ base: 12, lg: 9 }}>
          <TradingChartPanel onSymbolChange={setSelectedSymbol} height={chartHeight} />
        </Grid.Col>

        <Grid.Col span={{ base: 12, lg: 3 }}>
          <Stack gap="md" h="100%">
            <ChartDetailCard selectedSymbol={selectedSymbol} currency={currency} isLoading={isSelectedBarsLoading} metrics={detailMetrics} />
            <ChartsWatchlistCard rows={watchlistRows} selectedSymbol={selectedSymbol} currency={currency} onSelectSymbol={setSelectedSymbol} />
          </Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  )
}

export default ChartsPage
