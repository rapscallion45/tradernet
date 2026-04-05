import { FC, useMemo, useState } from "react"
import { Avatar, Badge, Divider, Grid, Group, Loader, Paper, ScrollArea, Stack, Table, Text, ThemeIcon } from "@mantine/core"
import { useQueries, useQuery } from "@tanstack/react-query"
import { IconChartCandle, IconStarFilled, IconTrendingDown, IconTrendingUp } from "@tabler/icons-react"
import { useViewportSize } from "@mantine/hooks"
import { getRestClient } from "api/ApiInterfaceAxios"
import { MarketBar } from "api/types"
import { TradingChartPanel } from "components/TradingChartPanel/TradingChartPanel"
import { QueryClientKeys } from "global/constants"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"
import { useMarketSymbols } from "hooks/useMarketSymbols"
import { formatCurrency } from "utils/intl"
import { getAssetLogoUrl, getBaseAsset } from "utils/marketAssets"

const watchlistLimit = 8

const getSymbolMetrics = (bars: MarketBar[]) => {
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

  const { data: selectedBars = [], isLoading: isSelectedBarsLoading } = useQuery({
    queryKey: [QueryClientKeys.MarketBars, selectedSymbol, currency, "charts-detail"],
    queryFn: () => getRestClient().marketResource.getBars(selectedSymbol, "1M", 24, currency),
    refetchInterval: 15_000,
  })

  const watchlistQueries = useQueries({
    queries: watchlistSymbols.map((symbol) => ({
      queryKey: [QueryClientKeys.MarketBars, symbol, currency, "charts-watchlist"],
      queryFn: () => getRestClient().marketResource.getBars(symbol, "1M", 2, currency),
      refetchInterval: 15_000,
    })),
  })

  const detailMetrics = useMemo(() => getSymbolMetrics(selectedBars), [selectedBars])

  const watchlistRows = watchlistSymbols.map((symbol, index) => {
    const bars = watchlistQueries[index]?.data ?? []
    const metrics = getSymbolMetrics(bars)
    return { symbol, ...metrics }
  })

  const isUp = detailMetrics.delta >= 0

  return (
    <Stack gap="md" h={`calc(100dvh - 120px)`}>
      <Grid gutter="md" align="stretch" style={{ flex: 1 }}>
        <Grid.Col span={{ base: 12, lg: 9 }}>
          <TradingChartPanel onSymbolChange={setSelectedSymbol} height={chartHeight} />
        </Grid.Col>

        <Grid.Col span={{ base: 12, lg: 3 }}>
          <Stack gap="md" h="100%">
            <Paper withBorder radius="md" p="md">
              <Group justify="space-between" align="flex-start" mb="sm">
                <Group gap="xs">
                  <Avatar src={getAssetLogoUrl(selectedSymbol)} alt={selectedSymbol} radius="xl" size={28}>
                    {getBaseAsset(selectedSymbol).slice(0, 1)}
                  </Avatar>
                  <div>
                    <Text fw={700}>{selectedSymbol}</Text>
                    <Text size="sm" c="dimmed">{`${currency} quote`}</Text>
                  </div>
                </Group>
                <Badge variant="light" color={isUp ? "green" : "red"}>{isUp ? "Bullish" : "Bearish"}</Badge>
              </Group>

              {isSelectedBarsLoading ? (
                <Group justify="center" py="md">
                  <Loader size="sm" />
                </Group>
              ) : (
                <Stack gap="sm">
                  <Group align="center" gap="xs">
                    <Text fz={32} fw={700}>{formatCurrency(detailMetrics.latest?.close ?? 0, currency)}</Text>
                    <ThemeIcon variant="light" color={isUp ? "green" : "red"} size="lg" radius="xl">
                      {isUp ? <IconTrendingUp size={18} /> : <IconTrendingDown size={18} />}
                    </ThemeIcon>
                  </Group>

                  <Text c={isUp ? "green" : "red"} fw={600}>{`${isUp ? "+" : ""}${detailMetrics.delta.toFixed(2)} (${detailMetrics.deltaPercent.toFixed(2)}%)`}</Text>

                  <Divider />

                  <Group justify="space-between">
                    <Text c="dimmed">24h High</Text>
                    <Text fw={500}>{formatCurrency(detailMetrics.dayHigh, currency)}</Text>
                  </Group>
                  <Group justify="space-between">
                    <Text c="dimmed">24h Low</Text>
                    <Text fw={500}>{formatCurrency(detailMetrics.dayLow, currency)}</Text>
                  </Group>
                  <Group justify="space-between">
                    <Text c="dimmed">24h Volume</Text>
                    <Text fw={500}>{detailMetrics.dayVolume.toLocaleString()}</Text>
                  </Group>
                </Stack>
              )}
            </Paper>

            <Paper withBorder radius="md" p="md" style={{ flex: 1, minHeight: 320 }}>
              <Group justify="space-between" mb="xs">
                <Group gap="xs">
                  <IconStarFilled size={16} />
                  <Text fw={700}>Watchlist</Text>
                </Group>
                <Badge variant="light">{watchlistRows.length}</Badge>
              </Group>
              <Divider mb="xs" />

              <ScrollArea h={320}>
                <Table striped highlightOnHover withTableBorder>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Symbol</Table.Th>
                      <Table.Th ta="right">Last</Table.Th>
                      <Table.Th ta="right">Chg %</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {watchlistRows.map((row) => {
                      const rowIsUp = row.delta >= 0
                      return (
                        <Table.Tr key={row.symbol} onClick={() => setSelectedSymbol(row.symbol)} style={{ cursor: "pointer" }}>
                          <Table.Td>
                            <Group gap="xs" wrap="nowrap">
                              <Avatar src={getAssetLogoUrl(row.symbol)} alt={row.symbol} radius="xl" size={20}>
                                {getBaseAsset(row.symbol).slice(0, 1)}
                              </Avatar>
                              <Text fw={row.symbol === selectedSymbol ? 700 : 500}>{row.symbol}</Text>
                            </Group>
                          </Table.Td>
                          <Table.Td ta="right">{formatCurrency(row.latest?.close ?? 0, currency)}</Table.Td>
                          <Table.Td ta="right" c={rowIsUp ? "green" : "red"}>{`${rowIsUp ? "+" : ""}${row.deltaPercent.toFixed(2)}%`}</Table.Td>
                        </Table.Tr>
                      )
                    })}
                  </Table.Tbody>
                </Table>
              </ScrollArea>
              <Group mt="sm" gap="xs" c="dimmed">
                <IconChartCandle size={14} />
                <Text size="xs">Click a row to focus that symbol in the detail panel.</Text>
              </Group>
            </Paper>
          </Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  )
}

export default ChartsPage
