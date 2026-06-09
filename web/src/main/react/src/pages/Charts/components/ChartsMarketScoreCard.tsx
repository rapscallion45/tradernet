import { FC, useMemo } from "react"
import { Badge, Box, Divider, Group, Loader, Paper, Progress, Stack, Text } from "@mantine/core"
import { IconActivityHeartbeat } from "@tabler/icons-react"
import { MarketContextSnapshot } from "api/types"

type ScoreFigure = {
  key: keyof MarketContextSnapshot
  label: string
  description: string
}

type ChartsMarketScoreCardProps = {
  selectedSymbol: string
  context?: MarketContextSnapshot
  isLoading: boolean
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
    description: "High positive funding can warn that long positioning is crowded.",
  },
  {
    key: "openInterestChangeZScore",
    label: "Open Interest",
    description: "Rising positioning can confirm stronger trend participation.",
  },
  {
    key: "mvrvZScore",
    label: "MVRV / Valuation",
    description: "Extreme positive valuation can signal overheated conditions.",
  },
  {
    key: "liquidityGrowthZScore",
    label: "Macro Liquidity",
    description: "Positive liquidity growth generally supports risk assets.",
  },
  {
    key: "sentimentZScore",
    label: "Sentiment",
    description: "Extreme optimism or fear can flag crowded positioning.",
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
}

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

const formatScore = (value: number) => `${value >= 0 ? "+" : ""}${value.toFixed(2)}`

const getScoreColor = (value: number) => {
  if (value > 0.25) return "green"
  if (value < -0.25) return "red"
  return "gray"
}

export const ChartsMarketScoreCard: FC<ChartsMarketScoreCardProps> = ({ selectedSymbol, context, isLoading }) => {
  const resolvedContext = context ?? neutralContext
  const populatedFigureCount = useMemo(
    () => scoreFigures.filter((figure) => Math.abs(resolvedContext[figure.key] ?? 0) > 0.001).length,
    [resolvedContext]
  )

  return (
    <Paper withBorder radius="md" p="md" style={{ display: "flex", flex: "1 1 0", flexDirection: "column", minHeight: 0, overflow: "hidden" }}>
      <Group justify="space-between" mb="xs" style={{ flexShrink: 0 }}>
        <Group gap="xs">
          <IconActivityHeartbeat size={16} />
          <div>
            <Text fw={700}>Market Score Inputs</Text>
            <Text size="xs" c="dimmed">{selectedSymbol}</Text>
          </div>
        </Group>
        <Badge variant="light">{populatedFigureCount}/{scoreFigures.length}</Badge>
      </Group>
      <Divider mb="xs" style={{ flexShrink: 0 }} />

      {isLoading ? (
        <Group justify="center" py="xl" style={{ flex: 1 }}>
          <Loader size="sm" />
        </Group>
      ) : (
        <Box style={{ flex: 1, minHeight: 0, overflowY: "auto", paddingRight: 4 }}>
          <Stack gap="sm">
            {scoreFigures.map((figure) => {
              const value = resolvedContext[figure.key] ?? 0
              const boundedValue = clamp(value, -2, 2)
              const progressValue = ((boundedValue + 2) / 4) * 100
              const color = getScoreColor(value)

              return (
                <Stack key={figure.key} gap={4}>
                  <Group justify="space-between" gap="xs" wrap="nowrap">
                    <div>
                      <Text size="sm" fw={600}>{figure.label}</Text>
                      <Text size="xs" c="dimmed">{figure.description}</Text>
                    </div>
                    <Badge color={color} variant="light">{formatScore(value)}</Badge>
                  </Group>
                  <Progress value={progressValue} color={color} size="sm" radius="xl" />
                </Stack>
              )
            })}
          </Stack>
        </Box>
      )}
    </Paper>
  )
}
