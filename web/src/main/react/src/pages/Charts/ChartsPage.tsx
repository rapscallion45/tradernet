import { FC, useMemo, useState } from "react"
import { Grid, Stack } from "@mantine/core"
import { useViewportSize } from "@mantine/hooks"
import { TradingChartPanel } from "components/TradingChartPanel/TradingChartPanel"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"
import { useChartDetailBars } from "hooks/useChartDetailBars"
import { getSymbolMetrics } from "utils/metrics"
import { ChartDetailCard } from "pages/Charts/components/ChartDetailCard"
import { useMarketContext } from "hooks/useMarketContext"
import { ChartsMarketScoreCard } from "pages/Charts/components/ChartsMarketScoreCard"

/**
 * Dedicated chart-focused page with market details and score inputs.
 */
const ChartsPage: FC = () => {
  const { currency } = useCurrencyPreference()
  const { height: viewportHeight } = useViewportSize()
  const [selectedSymbol, setSelectedSymbol] = useState("BTCUSDT")
  const chartHeight = Math.max(520, viewportHeight - 180)

  const { data: selectedBars = [], isLoading: isSelectedBarsLoading } = useChartDetailBars(selectedSymbol, currency)
  const { data: marketContext, isLoading: isMarketContextLoading } = useMarketContext(selectedSymbol)
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
            <ChartsMarketScoreCard selectedSymbol={selectedSymbol} context={marketContext} isLoading={isMarketContextLoading} />
          </Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  )
}

export default ChartsPage
