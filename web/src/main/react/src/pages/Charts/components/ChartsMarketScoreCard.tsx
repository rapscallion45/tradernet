import { FC, useMemo } from "react"
import { Badge, Box, Divider, Group, Loader, Paper, Progress, Stack, Text, Tooltip } from "@mantine/core"
import { IconActivityHeartbeat, IconInfoCircle } from "@tabler/icons-react"
import { MarketContextSnapshot } from "api/types"

type RawScoreFigureKey = Extract<keyof MarketContextSnapshot, `${string}ZScore`>
type BullishPercentKey = Extract<keyof MarketContextSnapshot, `${string}BullishPercent`>
type InputAvailableKey = Exclude<Extract<keyof MarketContextSnapshot, `${string}Available`>, "available" | "anyMarketScoreInputAvailable">

type ScoreFigure = {
  key: RawScoreFigureKey
  percentKey: BullishPercentKey
  availableKey: InputAvailableKey
  label: string
  description: string
}

type ChartsMarketScoreCardProps = {
  selectedSymbol: string
  context?: MarketContextSnapshot
  isLoading: boolean
  fillAvailable?: boolean
}

const scoreFigures: ScoreFigure[] = [
  {
    key: "etfFlowZScore",
    percentKey: "etfFlowBullishPercent",
    availableKey: "etfFlowAvailable",
    label: "ETF / Fund Flow",
    description: "Positive flow pressure is bullish when this asset has fund-flow data.",
  },
  {
    key: "exchangeOutflowZScore",
    percentKey: "exchangeOutflowBullishPercent",
    availableKey: "exchangeOutflowAvailable",
    label: "Exchange Outflow",
    description: "Positive values indicate net movement away from exchanges.",
  },
  {
    key: "fundingRateZScore",
    percentKey: "fundingRateBullishPercent",
    availableKey: "fundingRateAvailable",
    label: "Funding Rate",
    description: "Crowded positive funding is bearish; very negative funding can be contrarian bullish.",
  },
  {
    key: "openInterestChangeZScore",
    percentKey: "openInterestChangeBullishPercent",
    availableKey: "openInterestChangeAvailable",
    label: "Open Interest",
    description: "Rising positioning can confirm stronger trend participation.",
  },
  {
    key: "mvrvZScore",
    percentKey: "mvrvBullishPercent",
    availableKey: "mvrvAvailable",
    label: "MVRV / Valuation",
    description: "Overheated valuation is bearish; discounted valuation is bullish.",
  },
  {
    key: "liquidityGrowthZScore",
    percentKey: "liquidityGrowthBullishPercent",
    availableKey: "liquidityGrowthAvailable",
    label: "Macro Liquidity",
    description: "Positive liquidity growth generally supports risk assets.",
  },
  {
    key: "sentimentZScore",
    percentKey: "sentimentBullishPercent",
    availableKey: "sentimentAvailable",
    label: "Sentiment",
    description: "Extreme optimism can be bearish; extreme fear can be contrarian bullish.",
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
  etfFlowBullishPercent: 50,
  exchangeOutflowBullishPercent: 50,
  fundingRateBullishPercent: 50,
  openInterestChangeBullishPercent: 50,
  mvrvBullishPercent: 50,
  liquidityGrowthBullishPercent: 50,
  sentimentBullishPercent: 50,
  anyMarketScoreInputAvailable: false,
  etfFlowAvailable: false,
  exchangeOutflowAvailable: false,
  fundingRateAvailable: false,
  openInterestChangeAvailable: false,
  mvrvAvailable: false,
  liquidityGrowthAvailable: false,
  sentimentAvailable: false,
  available: false,
}

const formatRawScore = (value: number) => `${value >= 0 ? "+" : ""}${value.toFixed(2)}z`

const disabledBadgeStyle = { opacity: 0.55, filter: "grayscale(0.35)" }

const getScoreColor = (bullishPercent: number) => {
  if (bullishPercent > 56) return "green"
  if (bullishPercent < 44) return "red"
  return "gray"
}

const marketScoreHelp =
  "Percentages are calculated by the backend: 50% is neutral, higher supports BUY context, and lower supports SELL context. " +
  "These inputs roll up into the market score used by context-v2 signal scoring."

export const ChartsMarketScoreCard: FC<ChartsMarketScoreCardProps> = ({ selectedSymbol, context, isLoading, fillAvailable = true }) => {
  const resolvedContext = context ?? neutralContext
  const populatedFigureCount = useMemo(() => scoreFigures.filter((figure) => resolvedContext[figure.availableKey]).length, [resolvedContext])
  const hasMarketScoreInputs = resolvedContext.anyMarketScoreInputAvailable ?? populatedFigureCount > 0

  const paperStyle = fillAvailable
    ? { display: "flex", flex: "1 1 0", flexDirection: "column" as const, minHeight: 0, overflow: "hidden" }
    : { display: "flex", flexDirection: "column" as const }
  const contentStyle = fillAvailable ? { flex: 1, minHeight: 0, overflowY: "auto" as const, paddingRight: 4 } : { overflow: "visible" as const }

  return (
    <Paper withBorder radius="md" p="md" style={paperStyle}>
      <Group justify="space-between" mb="xs" style={{ flexShrink: 0 }}>
        <Group gap="xs">
          <IconActivityHeartbeat size={16} />
          <div>
            <Group gap={4} align="center">
              <Text fw={700}>Market Score Inputs</Text>
              <Tooltip label={marketScoreHelp} multiline w={300} withArrow>
                <Box component="span" c="dimmed" aria-label="Market score input help" style={{ display: "inline-flex", cursor: "help" }}>
                  <IconInfoCircle size={14} />
                </Box>
              </Tooltip>
            </Group>
            <Text size="xs" c="dimmed">
              {selectedSymbol}
            </Text>
          </div>
        </Group>
        <Badge color={hasMarketScoreInputs ? "blue" : "gray"} variant="light" style={hasMarketScoreInputs ? undefined : disabledBadgeStyle}>
          {hasMarketScoreInputs ? `${populatedFigureCount}/${scoreFigures.length}` : "No input data"}
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
            {!hasMarketScoreInputs && (
              <Text size="xs" c="dimmed">
                No market score input data has been loaded for this symbol yet. The backend still treats missing inputs as neutral for scoring, but the card
                does not display a bullish percentage until input data is present.
              </Text>
            )}
            {scoreFigures.map((figure) => {
              const value = resolvedContext[figure.key] ?? 0
              const inputAvailable = resolvedContext[figure.availableKey] ?? false
              const progressValue = resolvedContext[figure.percentKey] ?? 50
              const color = inputAvailable ? getScoreColor(progressValue) : "gray"

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
                      style={{ flex: "0 0 auto", minWidth: 86, textAlign: "center", ...(inputAvailable ? {} : disabledBadgeStyle) }}
                      title={inputAvailable ? `Raw input: ${formatRawScore(value)}` : `No backend input data for ${figure.label}`}>
                      {inputAvailable ? `${progressValue}% bull` : "No data"}
                    </Badge>
                  </Group>
                  <Progress value={inputAvailable ? progressValue : 50} color={color} size="sm" radius="xl" />
                </Stack>
              )
            })}
          </Stack>
        </Box>
      )}
    </Paper>
  )
}
