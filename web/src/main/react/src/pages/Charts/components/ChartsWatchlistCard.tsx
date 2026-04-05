import { FC } from "react"
import { Avatar, Badge, Divider, Group, Paper, ScrollArea, Table, Text } from "@mantine/core"
import { IconChartCandle, IconStarFilled } from "@tabler/icons-react"
import { SymbolMetrics } from "hooks/useMarketBars"
import { formatCurrency } from "utils/intl"
import { getAssetLogoUrl, getBaseAsset } from "utils/marketAssets"

type WatchlistRow = SymbolMetrics & {
  symbol: string
}

type ChartsWatchlistCardProps = {
  rows: WatchlistRow[]
  selectedSymbol: string
  currency: string
  onSelectSymbol: (symbol: string) => void
}

export const ChartsWatchlistCard: FC<ChartsWatchlistCardProps> = ({ rows, selectedSymbol, currency, onSelectSymbol }) => {
  return (
    <Paper withBorder radius="md" p="md" style={{ flex: 1, minHeight: 320 }}>
      <Group justify="space-between" mb="xs">
        <Group gap="xs">
          <IconStarFilled size={16} />
          <Text fw={700}>Watchlist</Text>
        </Group>
        <Badge variant="light">{rows.length}</Badge>
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
            {rows.map((row) => {
              const rowIsUp = row.delta >= 0
              return (
                <Table.Tr key={row.symbol} onClick={() => onSelectSymbol(row.symbol)} style={{ cursor: "pointer" }}>
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
  )
}
