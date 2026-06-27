import { FC, useMemo } from "react"
import { Badge, Box, Divider, Group, Loader, Paper, Progress, Stack, Text } from "@mantine/core"
import { IconActivityHeartbeat } from "@tabler/icons-react"
import { MarketContextSnapshot } from "api/types"

type ScoreFigureKey = Exclude<keyof MarketContextSnapshot, "available">

type ScoreFigure = {
  key: ScoreFigureKey
  label: string
  description: string
  signalScore?: (value: number) => number
}

type ChartsMarketScoreCardProps = {
  selectedSymbol: string
  context?: MarketContextSnapshot
  isLoading: boolean
  fillAvailable?: boolean
}

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

const fundingSignalScore = (value: number) => {
  if (value > 2) return -2
  if (value < -2) return 1
  if (value > 1) return clamp(-(value - 1), -2, 0)
  if (value < -1) return 0.5
  return 0
}

const valuationSignalScore = (value: number) => {
  if (value >= 2.5) return -2
  if (value >= 1.5) return -1
  if (value <= -1) return 1.5
  return 0.75
}

const sentimentSignalScore = (value: number) => {
  if (value >= 2) return -1
  if (value <= -2) return 1
  return clamp(value, -1, 1)
}

const scoreFigures: ScoreFigure[] = [
  {
    key: "etfFlowZScore",
    label: "ETF / Fund Flow",
    description: "Positive flow pressure is bullish when this asset has fund-flow data.",
  },
  {
    key: "exchangeOutflowZScore",
    label: "Exchange Outflow",
    description: "Positive values indicate net movement away from exchanges.",
  },
  {
    key: "fundingRateZScore",
    label: "Funding Rate",
    description: "Crowded positive funding is bearish; very negative funding can be contrarian bullish.",
    signalScore: fundingSignalScore,
  },
  {
    key: "openInterestChangeZScore",
    label: "Open Interest",
    description: "Rising positioning can confirm stronger trend participation.",
  },
  {
    key: "mvrvZScore",
    label: "MVRV / Valuation",
    description: "Overheated valuation is bearish; discounted valuation is bullish.",
    signalScore: valuationSignalScore,
  },
  {
    key: "liquidityGrowthZScore",
    label: "Macro Liquidity",
    description: "Positive liquidity growth generally supports risk assets.",
  },
  {
    key: "sentimentZScore",
    label: "Sentiment",
    description: "Extreme optimism can be bearish; extreme fear can be contrarian bullish.",
    signalScore: sentimentSignalScore,
  },
]

const neutralContext: MarketContextSnapshot = {
  etfFlowZScore: 0,
  exchangeOutflowZScore: 0,
  fundingRateZScore: 0,
  openInterestChangeZScore: 0,
  mvrvZScore: 0,
  liquidityGrowthZScore: 0,
  sentimentZScore: 0,
  available: false,
}

const toBullishPercent = (signalScore: number) => Math.round(((clamp(signalScore, -2, 2) + 2) / 4) * 100)

const formatRawScore = (value: number) => `${value >= 0 ? "+" : ""}${value.toFixed(2)}z`

const getScoreColor = (signalScore: number) => {
  if (signalScore > 0.25) return "green"
  if (signalScore < -0.25) return "red"
  return "gray"
}

export const ChartsMarketScoreCard: FC<ChartsMarketScoreCardProps> = ({ selectedSymbol, context, isLoading, fillAvailable = true }) => {
  const resolvedContext = context ?? neutralContext
  const populatedFigureCount = useMemo(
    () => scoreFigures.filter((figure) => Math.abs(resolvedContext[figure.key] ?? 0) > 0.001).length,
    [resolvedContext]
  )
  const hasMarketContext = resolvedContext.available ?? populatedFigureCount > 0

  const paperStyle = fillAvailable
    ? { display: "flex", flex: "1 1 0", flexDirection: "column" as const, minHeight: 0, overflow: "hidden" }
    : { display: "flex", flexDirection: "column" as const }
  const contentStyle = fillAvailable
    ? { flex: 1, minHeight: 0, overflowY: "auto" as const, paddingRight: 4 }
    : { overflow: "visible" as const }

  return (
    <Paper withBorder radius="md" p="md" style={paperStyle}>
      <Group justify="space-between" mb="xs" style={{ flexShrink: 0 }}>
        <Group gap="xs">
          <IconActivityHeartbeat size={16} />
          <div>
            <Text fw={700}>Market Score Inputs</Text>
            <Text size="xs" c="dimmed">{selectedSymbol}</Text>
          </div>
        </Group>
        <Badge color={hasMarketContext ? "blue" : "gray"} variant="light">
          {hasMarketContext ? `${populatedFigureCount}/${scoreFigures.length}` : "Awaiting data"}
        </Badge>
      </Group>
      <Divider mb="xs" style={{ flexShrink: 0 }} />

      {isLoading ? (
        <Group justify="center" py="xl" style={{ flex: 1 }}>
          <Loader size="sm" />
        </Group>
      ) : (
        <Box style={contentStyle}>
          <Stack gap="sm">
            <Text size="xs" c="dimmed">
              Percentages show each input&apos;s bullish tilt after normalization: 50% is neutral, higher supports BUY context, and lower
              supports SELL context. These inputs roll up into the market score used by context-v2 signal scoring.
            </Text>
            {!hasMarketContext && (
              <Text size="xs" c="dimmed">
                No market context has been loaded for this symbol yet. Signal scoring will treat these inputs as neutral until ingestion posts data.
              </Text>
            )}
            {scoreFigures.map((figure) => {
              const value = resolvedContext[figure.key] ?? 0
              const signalScore = figure.signalScore ? figure.signalScore(value) : clamp(value, -2, 2)
              const progressValue = toBullishPercent(signalScore)
              const color = hasMarketContext ? getScoreColor(signalScore) : "gray"

              return (
                <Stack key={figure.key} gap={4}>
                  <Group justify="space-between" align="flex-start" gap="xs" wrap="nowrap">
                    <Box style={{ flex: "1 1 auto", minWidth: 0, overflow: "hidden" }}>
                      <Text size="sm" fw={600} truncate>
                        {figure.label}
                      </Text>
                      <Text size="xs" c="dimmed" lineClamp={2}>
                        {figure.description}
                      </Text>
                    </Box>
                    <Badge
                      color={color}
                      variant="light"
                      style={{ flex: "0 0 auto", minWidth: 86, textAlign: "center" }}
                      title={`Raw input: ${formatRawScore(value)}`}
                    >
                      {hasMarketContext ? `${progressValue}% bull` : "—"}
                    </Badge>
                  </Group>
                  <Progress value={hasMarketContext ? progressValue : 50} color={color} size="sm" radius="xl" />
                </Stack>
              )
            })}
          </Stack>
        </Box>
      )}
    </Paper>
  )
}
