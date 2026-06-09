import { FC, useLayoutEffect, useMemo, useRef, useState } from "react"
import { Box, Grid, Stack } from "@mantine/core"
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
const APP_CONTENT_BOTTOM_PADDING = 16
const CHART_TOOLBAR_VERTICAL_OFFSET = 48

const ChartsPage: FC = () => {
  const { currency } = useCurrencyPreference()
  const { height: viewportHeight } = useViewportSize()
  const pageRef = useRef<HTMLDivElement>(null)
  const [selectedSymbol, setSelectedSymbol] = useState("BTCUSDT")
  const [pageHeight, setPageHeight] = useState(420)
  const chartHeight = Math.max(320, pageHeight - CHART_TOOLBAR_VERTICAL_OFFSET)

  useLayoutEffect(() => {
    const pageTop = pageRef.current?.getBoundingClientRect().top ?? 0
    setPageHeight(Math.max(420, viewportHeight - pageTop - APP_CONTENT_BOTTOM_PADDING))
  }, [viewportHeight])

  const { data: selectedBars = [], isLoading: isSelectedBarsLoading } = useChartDetailBars(selectedSymbol, currency)
  const { data: marketContext, isLoading: isMarketContextLoading } = useMarketContext(selectedSymbol)
  const detailMetrics = useMemo(() => getSymbolMetrics(selectedBars), [selectedBars])

  return (
    <Box ref={pageRef} h={pageHeight} mah={pageHeight} style={{ overflow: "hidden" }}>
      <Grid gutter="md" align="stretch" style={{ height: "100%", minHeight: 0, overflow: "hidden" }}>
        <Grid.Col span={{ base: 12, lg: 9 }} style={{ minHeight: 0, overflow: "hidden" }}>
          <TradingChartPanel onSymbolChange={setSelectedSymbol} height={chartHeight} />
        </Grid.Col>

        <Grid.Col span={{ base: 12, lg: 3 }} style={{ minHeight: 0, overflow: "hidden" }}>
          <Stack gap="md" h="100%" mih={0} style={{ overflow: "hidden" }}>
            <Box style={{ flexShrink: 0 }}>
              <ChartDetailCard selectedSymbol={selectedSymbol} currency={currency} isLoading={isSelectedBarsLoading} metrics={detailMetrics} />
            </Box>
            <ChartsMarketScoreCard selectedSymbol={selectedSymbol} context={marketContext} isLoading={isMarketContextLoading} />
          </Stack>
        </Grid.Col>
      </Grid>
    </Box>
  )
}

export default ChartsPage
