import { FC } from "react"
import { Badge, Group, Loader, Paper, Progress, Stack, Text, ThemeIcon } from "@mantine/core"
import { IconSparkles } from "@tabler/icons-react"
import { MarketForecast } from "api/types"

type ChartsForecastCardProps = {
  selectedSymbol: string
  forecast?: MarketForecast
  isLoading: boolean
  isError?: boolean
}

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

const getScoreColor = (score: number) => {
  if (score >= 70) return "green"
  if (score >= 55) return "blue"
  if (score >= 40) return "yellow"
  return "red"
}

export const ChartsForecastCard: FC<ChartsForecastCardProps> = ({ selectedSymbol, forecast, isLoading, isError }) => {
  const bullScore = clamp(forecast?.bullScore ?? 50, 0, 100)
  const probabilityPercent = Math.round(clamp(forecast?.probabilityPositiveReturn ?? 0.5, 0, 1) * 100)
  const scoreColor = getScoreColor(bullScore)
  const horizonDays = forecast?.horizonDays ?? 30

  return (
    <Paper withBorder radius="md" p="md">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="xs">
          <Group gap="xs">
            <ThemeIcon color={scoreColor} variant="light" radius="xl" size="lg">
              <IconSparkles size={18} />
            </ThemeIcon>
            <div>
              <Text fw={700}>Forecast</Text>
              <Text size="xs" c="dimmed">{selectedSymbol}</Text>
            </div>
          </Group>
          <Badge color={scoreColor} variant="light">
            {Math.round(bullScore)}% bull
          </Badge>
        </Group>

        {isLoading ? (
          <Group justify="center" py="sm">
            <Loader size="sm" />
          </Group>
        ) : isError ? (
          <Text size="sm" c="dimmed">
            Forecast is temporarily unavailable for this symbol.
          </Text>
        ) : (
          <Stack gap="xs">
            <Progress value={bullScore} color={scoreColor} size="sm" radius="xl" />
            <Text size="sm" c="dimmed">
              {forecast?.narrative || `Probability of a positive ${horizonDays}-day return: ${probabilityPercent}%.`}
            </Text>
            <Group justify="space-between" gap="xs">
              <Text size="xs" c="dimmed">Positive {horizonDays}d return</Text>
              <Text size="xs" fw={700}>{probabilityPercent}%</Text>
            </Group>
          </Stack>
        )}
      </Stack>
    </Paper>
  )
}
