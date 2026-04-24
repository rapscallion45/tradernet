import { FC, Suspense, useMemo } from "react"
import { Card, Group, Stack, Table, Text } from "@mantine/core"
import { Title } from "components/Title/Title"
import PageHeader from "components/layout/PageHeader/PageHeader"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import { usePortfolio } from "hooks/usePortfolio"
import { formatCurrency, formatDateTime, formatNumber } from "utils/intl"
import classes from "./PortfolioPage.module.css"

const CHART_WIDTH = 900
const CHART_HEIGHT = 220

const PortfolioChart: FC = () => {
  const { data: portfolio } = usePortfolio()

  const chart = useMemo(() => {
    const points = portfolio.history
    if (points.length === 0) {
      return { line: "", area: "", minY: 0, maxY: 0 }
    }

    const minY = Math.min(...points.map((point) => point.accountValue))
    const maxY = Math.max(...points.map((point) => point.accountValue))
    const rangeY = Math.max(maxY - minY, 1)

    const linePoints = points
      .map((point, index) => {
        const x = (index / Math.max(points.length - 1, 1)) * CHART_WIDTH
        const y = CHART_HEIGHT - ((point.accountValue - minY) / rangeY) * (CHART_HEIGHT - 20)
        return `${x},${y}`
      })
      .join(" ")

    const areaPoints = `${linePoints} ${CHART_WIDTH},${CHART_HEIGHT} 0,${CHART_HEIGHT}`
    return { line: linePoints, area: areaPoints, minY, maxY }
  }, [portfolio.history])

  if (portfolio.history.length === 0 || !chart.line) {
    return <Text c="dimmed">No portfolio history yet.</Text>
  }

  return (
    <Stack gap={6}>
      <svg viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} className={classes.chartSvg} role="img" aria-label="Portfolio value over time">
        <line x1={0} y1={CHART_HEIGHT} x2={CHART_WIDTH} y2={CHART_HEIGHT} className={classes.chartAxis} />
        <polygon points={chart.area} className={classes.chartArea} />
        <polyline points={chart.line} className={classes.chartLine} />
      </svg>
      <Group justify="space-between">
        <Text size="xs" c="dimmed">
          {formatDateTime(portfolio.history[0]?.timestamp)}
        </Text>
        <Text size="xs" c="dimmed">
          {formatDateTime(portfolio.history[portfolio.history.length - 1]?.timestamp)}
        </Text>
      </Group>
      <Group justify="space-between">
        <Text size="sm" fw={600}>
          Low: {formatCurrency(chart.minY, portfolio.currency)}
        </Text>
        <Text size="sm" fw={600}>
          High: {formatCurrency(chart.maxY, portfolio.currency)}
        </Text>
      </Group>
    </Stack>
  )
}

const PortfolioContent: FC = () => {
  const { data: portfolio } = usePortfolio()

  return (
    <Stack gap="lg">
      <Group grow>
        <Card withBorder>
          <Text size="sm" c="dimmed">
            Total Market Value
          </Text>
          <Text fw={700} fz={28}>
            {formatCurrency(portfolio.totalMarketValue, portfolio.currency)}
          </Text>
        </Card>
        <Card withBorder>
          <Text size="sm" c="dimmed">
            Total P/L
          </Text>
          <Text fw={700} fz={28} c={portfolio.totalProfitLoss >= 0 ? "green" : "red"}>
            {`${formatCurrency(portfolio.totalProfitLoss, portfolio.currency)} (${portfolio.totalProfitLossPercent.toFixed(2)}%)`}
          </Text>
        </Card>
      </Group>

      <Card withBorder>
        <Text fw={700} mb="sm">
          Account value over time
        </Text>
        <PortfolioChart />
      </Card>

      <Card withBorder>
        <Text fw={700} mb="sm">
          Held currencies
        </Text>
        <Table striped highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Asset</Table.Th>
              <Table.Th ta="right">Quantity</Table.Th>
              <Table.Th ta="right">Avg Cost</Table.Th>
              <Table.Th ta="right">Current Price</Table.Th>
              <Table.Th ta="right">Market Value</Table.Th>
              <Table.Th ta="right">P/L</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {portfolio.assets.map((asset) => (
              <Table.Tr key={asset.symbol}>
                <Table.Td>{asset.symbol}</Table.Td>
                <Table.Td ta="right">{formatNumber(asset.quantity, { maximumFractionDigits: 8 })}</Table.Td>
                <Table.Td ta="right">{formatCurrency(asset.averageCost, portfolio.currency)}</Table.Td>
                <Table.Td ta="right">{formatCurrency(asset.currentPrice, portfolio.currency)}</Table.Td>
                <Table.Td ta="right">{formatCurrency(asset.marketValue, portfolio.currency)}</Table.Td>
                <Table.Td ta="right" c={asset.profitLoss >= 0 ? "green" : "red"}>{`${formatCurrency(asset.profitLoss, portfolio.currency)} (${asset.profitLossPercent.toFixed(2)}%)`}</Table.Td>
              </Table.Tr>
            ))}
            {portfolio.assets.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={6}>
                  <Text size="sm" c="dimmed" ta="center">
                    No held currencies yet. Place an order to start building your portfolio.
                  </Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      </Card>
    </Stack>
  )
}

const PortfolioPage: FC = () => {
  return (
    <Stack gap="xl">
      <PageHeader title={<Title>Portfolio</Title>} description="Track currently held currencies, P/L, and account value growth." />
      <Suspense fallback={<PageLoadingSkeleton />}>
        <PortfolioContent />
      </Suspense>
    </Stack>
  )
}

export default PortfolioPage
