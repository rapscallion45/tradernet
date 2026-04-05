import { FC } from "react"
import { Avatar, Badge, Divider, Group, Loader, Paper, Stack, Text, ThemeIcon } from "@mantine/core"
import { IconTrendingDown, IconTrendingUp } from "@tabler/icons-react"
import { SymbolMetrics } from "utils/metrics"
import { formatCurrency } from "utils/intl"
import { getAssetLogoUrl, getBaseAsset } from "utils/marketAssets"

type ChartDetailCardProps = {
  selectedSymbol: string
  currency: string
  isLoading: boolean
  metrics: SymbolMetrics
}

export const ChartDetailCard: FC<ChartDetailCardProps> = ({ selectedSymbol, currency, isLoading, metrics }) => {
  const isUp = metrics.delta >= 0

  return (
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

      {isLoading ? (
        <Group justify="center" py="md">
          <Loader size="sm" />
        </Group>
      ) : (
        <Stack gap="sm">
          <Group align="center" gap="xs">
            <Text fz={32} fw={700}>{formatCurrency(metrics.latest?.close ?? 0, currency)}</Text>
            <ThemeIcon variant="light" color={isUp ? "green" : "red"} size="lg" radius="xl">
              {isUp ? <IconTrendingUp size={18} /> : <IconTrendingDown size={18} />}
            </ThemeIcon>
          </Group>

          <Text c={isUp ? "green" : "red"} fw={600}>{`${isUp ? "+" : ""}${metrics.delta.toFixed(2)} (${metrics.deltaPercent.toFixed(2)}%)`}</Text>

          <Divider />

          <Group justify="space-between">
            <Text c="dimmed">24h High</Text>
            <Text fw={500}>{formatCurrency(metrics.dayHigh, currency)}</Text>
          </Group>
          <Group justify="space-between">
            <Text c="dimmed">24h Low</Text>
            <Text fw={500}>{formatCurrency(metrics.dayLow, currency)}</Text>
          </Group>
          <Group justify="space-between">
            <Text c="dimmed">24h Volume</Text>
            <Text fw={500}>{metrics.dayVolume.toLocaleString()}</Text>
          </Group>
        </Stack>
      )}
    </Paper>
  )
}
