import { FC } from "react"
import { Badge, Box, Divider, Group, Loader, Paper, SimpleGrid, Stack, Text, ThemeIcon, Tooltip } from "@mantine/core"
import { IconChartBar, IconInfoCircle } from "@tabler/icons-react"
import { OrderBookLevel, OrderBookSnapshot, OrderBookStatus } from "api/types"
import { formatCurrency, formatNumber } from "utils/intl"

type ChartsOrderBookCardProps = {
  selectedSymbol: string
  currency: string
  orderBook?: OrderBookSnapshot
  isLoading: boolean
  isError?: boolean
}

type OrderBookRowsProps = {
  levels: OrderBookLevel[]
  side: "bid" | "ask"
  currency: string
}

const statusColor: Record<OrderBookStatus, string> = {
  LIVE: "green",
  SYNCING: "gray",
  SNAPSHOT_ONLY: "blue",
  STALE: "yellow",
  UNAVAILABLE: "red",
}

const statusLabel: Record<OrderBookStatus, string> = {
  LIVE: "Live",
  SYNCING: "Syncing",
  SNAPSHOT_ONLY: "Snapshot",
  STALE: "Stale",
  UNAVAILABLE: "Offline",
}

const orderBookHelp = "Backend-maintained Binance aggregated L2 depth. The server syncs a REST snapshot with diff-depth updates and resyncs if update IDs gap."

const formatQuantity = (value: number) =>
  formatNumber(value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 6,
  })

const formatPercent = (value: number) => `${value >= 0 ? "+" : ""}${value.toFixed(1)}%`

const LevelRows: FC<OrderBookRowsProps> = ({ levels, side, currency }) => {
  const rows = side === "ask" ? levels.slice().reverse() : levels
  const depthColor = side === "bid" ? "rgba(47, 158, 68, 0.14)" : "rgba(224, 49, 49, 0.14)"
  const priceColor = side === "bid" ? "green" : "red"

  return (
    <Stack gap={3}>
      {rows.map((level) => (
        <Box
          key={`${side}-${level.price}`}
          style={{
            position: "relative",
            minHeight: 24,
            borderRadius: 4,
            overflow: "hidden",
          }}>
          <Box
            aria-hidden
            style={{
              position: "absolute",
              top: 0,
              right: 0,
              bottom: 0,
              width: `${Math.max(2, Math.min(100, level.depthPercent))}%`,
              background: depthColor,
            }}
          />
          <Box
            style={{
              position: "relative",
              zIndex: 1,
              display: "grid",
              gridTemplateColumns: "minmax(82px, 1fr) minmax(68px, 0.8fr) minmax(82px, 1fr)",
              columnGap: 8,
              alignItems: "center",
              minHeight: 24,
              padding: "0 4px",
            }}>
            <Text size="xs" fw={700} c={priceColor} truncate>
              {formatCurrency(level.price, currency)}
            </Text>
            <Text size="xs" ta="right" truncate>
              {formatQuantity(level.quantity)}
            </Text>
            <Text size="xs" ta="right" c="dimmed" truncate>
              {formatCurrency(level.cumulativeNotional, currency)}
            </Text>
          </Box>
        </Box>
      ))}
    </Stack>
  )
}

export const ChartsOrderBookCard: FC<ChartsOrderBookCardProps> = ({ selectedSymbol, currency, orderBook, isLoading, isError }) => {
  const status = orderBook?.status ?? "SYNCING"
  const imbalance = orderBook?.depthImbalancePercent ?? 0
  const imbalanceColor = imbalance > 5 ? "green" : imbalance < -5 ? "red" : "gray"
  const hasRows = Boolean(orderBook?.bids.length || orderBook?.asks.length)

  return (
    <Paper withBorder radius="md" p="md">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="xs">
          <Group gap="xs">
            <ThemeIcon color={statusColor[status]} variant="light" radius="xl" size="lg">
              <IconChartBar size={18} />
            </ThemeIcon>
            <div>
              <Group gap={4} align="center">
                <Text fw={700}>Order Book</Text>
                <Tooltip label={orderBookHelp} multiline w={300} withArrow>
                  <Box component="span" c="dimmed" aria-label="Order book sync details" style={{ display: "inline-flex", cursor: "help" }}>
                    <IconInfoCircle size={14} />
                  </Box>
                </Tooltip>
              </Group>
              <Text size="xs" c="dimmed">
                {selectedSymbol}
              </Text>
            </div>
          </Group>
          <Group gap={4} justify="flex-end">
            <Badge color={statusColor[status]} variant="light">
              {statusLabel[status]}
            </Badge>
            <Badge color="gray" variant="outline">
              L2
            </Badge>
          </Group>
        </Group>

        {isLoading ? (
          <Group justify="center" py="md">
            <Loader size="sm" />
          </Group>
        ) : isError || !orderBook ? (
          <Text size="sm" c="dimmed">
            Order book is temporarily unavailable for this symbol.
          </Text>
        ) : (
          <>
            <SimpleGrid cols={2} spacing="xs">
              <Box>
                <Text size="xs" c="dimmed">
                  Mid
                </Text>
                <Text size="sm" fw={700} truncate>
                  {formatCurrency(orderBook.midPrice, currency)}
                </Text>
              </Box>
              <Box>
                <Text size="xs" c="dimmed">
                  Spread
                </Text>
                <Text size="sm" fw={700} truncate>
                  {formatCurrency(orderBook.spread, currency)} ({orderBook.spreadPercent.toFixed(3)}%)
                </Text>
              </Box>
              <Box>
                <Text size="xs" c="dimmed">
                  Bid depth
                </Text>
                <Text size="sm" fw={700} c="green" truncate>
                  {formatCurrency(orderBook.bidDepthNotional, currency)}
                </Text>
              </Box>
              <Box>
                <Text size="xs" c="dimmed">
                  Imbalance
                </Text>
                <Text size="sm" fw={700} c={imbalanceColor} truncate>
                  {formatPercent(imbalance)}
                </Text>
              </Box>
            </SimpleGrid>

            {status !== "LIVE" && (
              <Text size="xs" c="dimmed" lineClamp={2}>
                {orderBook.message}
              </Text>
            )}

            <Divider />

            {hasRows ? (
              <Stack gap="xs">
                <Box
                  style={{
                    display: "grid",
                    gridTemplateColumns: "minmax(82px, 1fr) minmax(68px, 0.8fr) minmax(82px, 1fr)",
                    columnGap: 8,
                    padding: "0 4px",
                  }}>
                  <Text size="xs" c="dimmed">
                    Price
                  </Text>
                  <Text size="xs" c="dimmed" ta="right">
                    Size
                  </Text>
                  <Text size="xs" c="dimmed" ta="right">
                    Total
                  </Text>
                </Box>
                <LevelRows levels={orderBook.asks} side="ask" currency={currency} />
                <Group
                  justify="space-between"
                  px={4}
                  py={2}
                  style={{ borderTop: "1px solid var(--mantine-color-default-border)", borderBottom: "1px solid var(--mantine-color-default-border)" }}>
                  <Text size="xs" c="dimmed">
                    Best bid / ask
                  </Text>
                  <Text size="xs" fw={700}>
                    {formatCurrency(orderBook.bestBid, currency)} / {formatCurrency(orderBook.bestAsk, currency)}
                  </Text>
                </Group>
                <LevelRows levels={orderBook.bids} side="bid" currency={currency} />
              </Stack>
            ) : (
              <Text size="sm" c="dimmed">
                Waiting for Binance depth data.
              </Text>
            )}
          </>
        )}
      </Stack>
    </Paper>
  )
}
