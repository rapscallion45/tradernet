import { FC } from "react"
import { Badge, Group, Loader, Paper, Progress, Select, Stack, Text, ThemeIcon } from "@mantine/core"
import { IconSparkles } from "@tabler/icons-react"
import { MarketForecast } from "api/types"

type ChartsForecastCardProps = {
  selectedSymbol: string
  horizonDays: number
  onHorizonDaysChange: (horizonDays: number) => void
  forecast?: MarketForecast
  isLoading: boolean
  isError?: boolean
}

const forecastHorizonOptions = [
  { value: "1", label: "1 day" },
  { value: "3", label: "3 days" },
  { value: "7", label: "7 days" },
  { value: "14", label: "14 days" },
  { value: "30", label: "30 days" },
]

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

const getScoreColor = (score: number) => {
  if (score >= 70) return "green"
  if (score >= 55) return "blue"
  if (score >= 40) return "yellow"
  return "red"
}

export const ChartsForecastCard: FC<ChartsForecastCardProps> = ({ selectedSymbol, horizonDays, onHorizonDaysChange, forecast, isLoading, isError }) => {
  const bullScore = clamp(forecast?.bullScore ?? 50, 0, 100)
  const probabilityPercent = Math.round(clamp(forecast?.probabilityPositiveReturn ?? 0.5, 0, 1) * 100)
  const scoreColor = getScoreColor(bullScore)
  const selectedHorizonDays = forecast?.horizonDays ?? horizonDays

  return (
    <Paper withBorder radius="md" p="md">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="xs">
          <Group gap="xs">
            <ThemeIcon color={scoreColor} variant="light" radius="xl" size="lg">
              <IconSparkles size={18} />
            </ThemeIcon>
            <div>
              <Text fw={700}>TradernetAI Forecast</Text>
              <Text size="xs" c="dimmed">
                {selectedSymbol}
              </Text>
            </div>
          </Group>
          <Badge color={scoreColor} variant="light">
            {Math.round(bullScore)}% bull
          </Badge>
        </Group>

        <Select
          label="Forecast horizon"
          aria-label="Select forecast horizon"
          data={forecastHorizonOptions}
          value={String(horizonDays)}
          onChange={(value) => {
            if (value) {
              onHorizonDaysChange(Number(value))
            }
          }}
          allowDeselect={false}
          size="xs"
        />

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
              {forecast?.narrative || `Probability of a positive ${selectedHorizonDays}-day return: ${probabilityPercent}%.`}
            </Text>
            {forecast?.marketConditionSummary && (
              <Text size="sm">
                <Text span fw={700}>
                  Current condition:{" "}
                </Text>
                {forecast.marketConditionSummary}
              </Text>
            )}
            <Group justify="space-between" gap="xs">
              <Text size="xs" c="dimmed">
                Positive {selectedHorizonDays}d return
              </Text>
              <Text size="xs" fw={700}>
                {probabilityPercent}%
              </Text>
            </Group>
          </Stack>
        )}
      </Stack>
    </Paper>
  )
}
