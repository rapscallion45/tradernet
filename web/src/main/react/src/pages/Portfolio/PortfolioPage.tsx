import { FC, PointerEvent, Suspense, useMemo, useState } from "react"
import { Card, Group, Stack, Text } from "@mantine/core"
import { ColumnDef } from "@tanstack/react-table"
import { PortfolioHistoryEvent, PortfolioHistoryPoint } from "api/types"
import { Title } from "components/Title/Title"
import PageHeader from "components/layout/PageHeader/PageHeader"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import { Table } from "components/Table/Table"
import { usePortfolio } from "hooks/usePortfolio"
import { formatCurrency, formatDateTime, formatNumber } from "utils/intl"
import classes from "./PortfolioPage.module.css"

const CHART_WIDTH = 900
const CHART_HEIGHT = 220
const CHART_MARGIN = {
  top: 12,
  right: 16,
  bottom: 32,
  left: 82,
}
const CHART_PLOT_WIDTH = CHART_WIDTH - CHART_MARGIN.left - CHART_MARGIN.right
const CHART_PLOT_HEIGHT = CHART_HEIGHT - CHART_MARGIN.top - CHART_MARGIN.bottom
const CHART_PLOT_BOTTOM = CHART_MARGIN.top + CHART_PLOT_HEIGHT
const CHART_PLOT_RIGHT = CHART_MARGIN.left + CHART_PLOT_WIDTH

type ChartRenderPoint = {
  x: number
  y: number
  point: PortfolioHistoryPoint
}

type ChartTick = {
  x?: number
  y?: number
  label: string
  textAnchor?: "start" | "middle" | "end"
}

const getEventMarkerClassName = (events: PortfolioHistoryEvent[] = []) => {
  const hasBuy = events.some((event) => event.side === "BUY")
  const hasSell = events.some((event) => event.side === "SELL")

  if (hasBuy && hasSell) {
    return `${classes.chartEventMarker} ${classes.chartEventMarkerMixed}`
  }

  if (hasSell) {
    return `${classes.chartEventMarker} ${classes.chartEventMarkerSell}`
  }

  return `${classes.chartEventMarker} ${classes.chartEventMarkerBuy}`
}

const getEvenIndexes = (length: number, tickCount: number) => {
  if (length <= 0) {
    return []
  }

  const effectiveTickCount = Math.min(length, tickCount)
  if (effectiveTickCount === 1) {
    return [0]
  }

  return Array.from(
    new Set(
      Array.from({ length: effectiveTickCount }, (_, index) => Math.round((index / (effectiveTickCount - 1)) * (length - 1))),
    ),
  )
}

const formatAxisDate = (timestamp: number, includeYear: boolean) => {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return ""
  }

  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: includeYear ? "2-digit" : undefined,
  }).format(date)
}

const formatAxisCurrency = (value: number, currency: string) => {
  try {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency,
      notation: "compact",
      maximumFractionDigits: 1,
    }).format(value)
  } catch {
    return formatCurrency(value, currency)
  }
}

const PortfolioChart: FC = () => {
  const { data: portfolio } = usePortfolio()
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  const chart = useMemo(() => {
    const points = portfolio.history
    if (points.length === 0) {
      return { line: "", area: "", minY: 0, maxY: 0, points: [] as ChartRenderPoint[], xTicks: [] as ChartTick[], yTicks: [] as ChartTick[] }
    }

    const minY = Math.min(...points.map((point) => point.accountValue))
    const maxY = Math.max(...points.map((point) => point.accountValue))
    const rawRangeY = maxY - minY
    const yPadding = rawRangeY === 0 ? Math.max(Math.abs(maxY) * 0.05, 1) : rawRangeY * 0.08
    const axisMinY = Math.max(0, minY - yPadding)
    const axisMaxY = maxY + yPadding
    const rangeY = Math.max(axisMaxY - axisMinY, 1)
    const minX = points[0]?.timestamp ?? 0
    const maxX = points[points.length - 1]?.timestamp ?? minX
    const rangeX = Math.max(maxX - minX, 1)

    const renderPoints = points.map((point, index) => {
      const x = points.length === 1 ? CHART_MARGIN.left : CHART_MARGIN.left + ((point.timestamp - minX) / rangeX) * CHART_PLOT_WIDTH
      const y = CHART_MARGIN.top + ((axisMaxY - point.accountValue) / rangeY) * CHART_PLOT_HEIGHT
      return { x, y, point }
    })

    const linePoints = renderPoints
      .map((point) => `${point.x},${point.y}`)
      .join(" ")

    const areaPoints = `${linePoints} ${CHART_PLOT_RIGHT},${CHART_PLOT_BOTTOM} ${CHART_MARGIN.left},${CHART_PLOT_BOTTOM}`
    const includeYear = new Date(minX).getFullYear() !== new Date(maxX).getFullYear()
    const xTicks = getEvenIndexes(renderPoints.length, 5).map((index) => {
      const point = renderPoints[index]
      return {
        x: point.x,
        label: formatAxisDate(point.point.timestamp, includeYear),
        textAnchor: index === 0 ? "start" : index === renderPoints.length - 1 ? "end" : "middle",
      }
    })

    const yValues = rawRangeY === 0 ? [minY] : [axisMaxY, axisMinY + rangeY * (2 / 3), axisMinY + rangeY * (1 / 3), axisMinY]
    const yTicks = yValues.map((value) => ({
      y: CHART_MARGIN.top + ((axisMaxY - value) / rangeY) * CHART_PLOT_HEIGHT,
      label: formatAxisCurrency(value, portfolio.currency),
    }))

    return { line: linePoints, area: areaPoints, minY, maxY, points: renderPoints, xTicks, yTicks }
  }, [portfolio.currency, portfolio.history])

  if (portfolio.history.length === 0 || !chart.line) {
    return <Text c="dimmed">No portfolio history yet.</Text>
  }

  const hoveredPoint = hoverIndex === null ? null : chart.points[hoverIndex]
  const tooltipAlign = hoveredPoint && hoveredPoint.x > CHART_WIDTH * 0.72 ? "right" : hoveredPoint && hoveredPoint.x < CHART_WIDTH * 0.28 ? "left" : "center"
  const tooltipPlacement = hoveredPoint && hoveredPoint.y < 96 ? "below" : "above"

  const handlePointerMove = (event: PointerEvent<SVGRectElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect()
    const relativeX = Math.min(Math.max(event.clientX - bounds.left, 0), bounds.width)
    const chartX = bounds.width === 0 ? CHART_MARGIN.left : CHART_MARGIN.left + (relativeX / bounds.width) * CHART_PLOT_WIDTH
    const index = chart.points.reduce((nearestIndex, point, pointIndex) => {
      return Math.abs(point.x - chartX) < Math.abs(chart.points[nearestIndex].x - chartX) ? pointIndex : nearestIndex
    }, 0)
    setHoverIndex(index)
  }

  return (
    <Stack gap={6}>
      <div className={classes.chartFrame}>
        <svg viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} className={classes.chartSvg} role="img" aria-label="Portfolio value over time">
          {chart.yTicks.map((tick) => (
            <g key={`y-${tick.label}-${tick.y}`}>
              <line x1={CHART_MARGIN.left} y1={tick.y} x2={CHART_PLOT_RIGHT} y2={tick.y} className={classes.chartGrid} />
              <text x={CHART_MARGIN.left - 8} y={(tick.y ?? 0) + 4} textAnchor="end" className={classes.chartTickLabel}>
                {tick.label}
              </text>
            </g>
          ))}
          <line x1={CHART_MARGIN.left} y1={CHART_MARGIN.top} x2={CHART_MARGIN.left} y2={CHART_PLOT_BOTTOM} className={classes.chartAxis} />
          <line x1={CHART_MARGIN.left} y1={CHART_PLOT_BOTTOM} x2={CHART_PLOT_RIGHT} y2={CHART_PLOT_BOTTOM} className={classes.chartAxis} />
          <text
            x={14}
            y={CHART_MARGIN.top + CHART_PLOT_HEIGHT / 2}
            transform={`rotate(-90 14 ${CHART_MARGIN.top + CHART_PLOT_HEIGHT / 2})`}
            textAnchor="middle"
            className={classes.chartAxisLabel}>
            Portfolio value
          </text>
          <polygon points={chart.area} className={classes.chartArea} />
          <polyline points={chart.line} className={classes.chartLine} />
          {chart.points.map(({ x, y, point }) =>
            point.events?.length ? <circle key={point.timestamp} cx={x} cy={y} r={4.5} className={getEventMarkerClassName(point.events)} /> : null,
          )}
          {hoveredPoint ? (
            <>
              <line x1={hoveredPoint.x} y1={CHART_MARGIN.top} x2={hoveredPoint.x} y2={CHART_PLOT_BOTTOM} className={classes.chartCrosshair} />
              <circle cx={hoveredPoint.x} cy={hoveredPoint.y} r={5} className={classes.chartHoverPoint} />
            </>
          ) : null}
          {chart.xTicks.map((tick) => (
            <g key={`x-${tick.label}-${tick.x}`}>
              <line x1={tick.x} y1={CHART_PLOT_BOTTOM} x2={tick.x} y2={CHART_PLOT_BOTTOM + 5} className={classes.chartTick} />
              <text x={tick.x} y={CHART_PLOT_BOTTOM + 22} textAnchor={tick.textAnchor} className={classes.chartTickLabel}>
                {tick.label}
              </text>
            </g>
          ))}
          <rect
            x={CHART_MARGIN.left}
            y={CHART_MARGIN.top}
            width={CHART_PLOT_WIDTH}
            height={CHART_PLOT_HEIGHT}
            className={classes.chartHitArea}
            onPointerMove={handlePointerMove}
            onPointerLeave={() => setHoverIndex(null)}
          />
        </svg>
        {hoveredPoint ? (
          <div
            className={classes.chartTooltip}
            data-align={tooltipAlign}
            data-placement={tooltipPlacement}
            style={{
              left: `${(hoveredPoint.x / CHART_WIDTH) * 100}%`,
              top: `${(hoveredPoint.y / CHART_HEIGHT) * 100}%`,
            }}>
            <Text size="xs" fw={700}>
              {formatDateTime(hoveredPoint.point.timestamp)}
            </Text>
            <Text size="sm" fw={700}>
              {formatCurrency(hoveredPoint.point.accountValue, portfolio.currency)}
            </Text>
            {hoveredPoint.point.events?.map((event) => (
              <Text key={`${event.timestamp}-${event.symbol}-${event.side}`} size="xs" c={event.side === "SELL" ? "red" : "green"}>
                {`${event.side} ${formatNumber(event.quantity, { maximumFractionDigits: 8 })} ${event.symbol} @ ${formatCurrency(event.price, portfolio.currency)}`}
              </Text>
            ))}
          </div>
        ) : null}
      </div>
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

type PortfolioAssetRow = {
  id: string
  symbol: string
  quantity: number
  averageCost: number
  currentPrice: number
  marketValue: number
  profitLoss: number
  profitLossPercent: number
}

const PortfolioContent: FC = () => {
  const { data: portfolio } = usePortfolio()

  const rows = useMemo<PortfolioAssetRow[]>(
    () =>
      portfolio.assets.map((asset) => ({
        id: asset.symbol,
        symbol: asset.symbol,
        quantity: asset.quantity,
        averageCost: asset.averageCost,
        currentPrice: asset.currentPrice,
        marketValue: asset.marketValue,
        profitLoss: asset.profitLoss,
        profitLossPercent: asset.profitLossPercent,
      })),
    [portfolio.assets],
  )

  const columns = useMemo<ColumnDef<PortfolioAssetRow>[]>(
    () => [
      { accessorKey: "symbol", header: "Asset" },
      {
        accessorKey: "quantity",
        header: "Quantity",
        cell: ({ row }) => formatNumber(row.original.quantity, { maximumFractionDigits: 8 }),
      },
      {
        accessorKey: "averageCost",
        header: "Avg Cost",
        cell: ({ row }) => formatCurrency(row.original.averageCost, portfolio.currency),
      },
      {
        accessorKey: "currentPrice",
        header: "Current Price",
        cell: ({ row }) => formatCurrency(row.original.currentPrice, portfolio.currency),
      },
      {
        accessorKey: "marketValue",
        header: "Market Value",
        cell: ({ row }) => formatCurrency(row.original.marketValue, portfolio.currency),
      },
      {
        accessorKey: "profitLoss",
        header: "P/L",
        cell: ({ row }) => {
          const value = row.original.profitLoss
          const color = value >= 0 ? "green" : "red"
          return <Text c={color}>{`${formatCurrency(value, portfolio.currency)} (${row.original.profitLossPercent.toFixed(2)}%)`}</Text>
        },
      },
    ],
    [portfolio.currency],
  )

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
        <Table<PortfolioAssetRow> columns={columns} data={rows} caption={rows.length === 0 ? "No held currencies yet. Place an order to start building your portfolio." : undefined} />
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
