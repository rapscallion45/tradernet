import { FC, MouseEvent, MutableRefObject, useEffect, useMemo, useRef, useState } from "react"
import { Badge, Button, Group, Modal, Paper, SegmentedControl, Select, Stack, Text, TextInput, useMantineColorScheme } from "@mantine/core"
import uPlot, { AlignedData, Options, Plugin } from "uplot"
import "uplot/dist/uPlot.min.css"
import classes from "./TradingChartPanel.module.css"
import { useToast } from "hooks/useToast"
import { DEFAULT_CHART_SYMBOL } from "global/constants"
import { formatCurrency, formatDateTime } from "utils/intl"
import { useMarketSymbols } from "hooks/useMarketSymbols"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"

type Candle = {
  time: number
  open: number
  high: number
  low: number
  close: number
  ema: number
}

type DrawTool = "none" | "trendline" | "ray" | "hline" | "vline"

type ChartPoint = {
  time: number
  price: number
}

type Drawing =
  | { id: string; type: "hline"; y: number }
  | { id: string; type: "vline"; time: number }
  | { id: string; type: "trendline"; start: ChartPoint; end: ChartPoint }
  | { id: string; type: "ray"; start: ChartPoint; end: ChartPoint }

type SignalSide = "BUY" | "SELL" | "HOLD"

type ChartSignal = {
  symbol: string
  eventTime: number
  side: SignalSide
  confidence: number
  modelVersion: string
  notes: string[]
}

type WorkerPayload = {
  type: "bars"
  payload: {
    symbol: string
    candles: Candle[]
    lastPrice: number
    ticksPerSecond: number
    status: "connected" | "disconnected" | "error"
    error?: string
    signal?: ChartSignal | null
  }
}

type CandleArrays = {
  x: number[]
  open: number[]
  high: number[]
  low: number[]
  close: number[]
  ema: number[]
  sma20: Array<number | null>
  bbUpper: Array<number | null>
  bbLower: Array<number | null>
}

type Indicators = {
  ema: boolean
  sma: boolean
  bb: boolean
}

const chartHeight = 420

const intervalPresets = ["1S", "5S", "15S", "30S", "1M", "5M", "15M", "1H", "4H", "1D", "1MO", "1Y"] as const

const intervalPattern = /^(\d+)(S|M|H|D|MO|Y)$/

const intervalUnitMs: Record<string, number> = {
  S: 1_000,
  M: 60_000,
  H: 3_600_000,
  D: 86_400_000,
  MO: 2_592_000_000,
  Y: 31_536_000_000,
}

const normalizeIntervalToken = (token: string): string => {
  const normalized = token.trim().toUpperCase()
  return intervalPattern.test(normalized) ? normalized : "1S"
}

const intervalTokenToMs = (token: string): number => {
  const parsed = normalizeIntervalToken(token).match(intervalPattern)
  if (!parsed) {
    return 1_000
  }

  const amount = Number(parsed[1])
  const unit = parsed[2]
  const multiplier = intervalUnitMs[unit] || 1_000
  return Math.max(1_000, amount * multiplier)
}
const mean = (values: number[]) => values.reduce((acc, value) => acc + value, 0) / values.length

const toCandleArrays = (candles: Candle[]): CandleArrays => {
  const close = candles.map((candle) => candle.close)
  const sma20: Array<number | null> = []
  const bbUpper: Array<number | null> = []
  const bbLower: Array<number | null> = []

  for (let index = 0; index < close.length; index += 1) {
    if (index < 19) {
      sma20.push(null)
      bbUpper.push(null)
      bbLower.push(null)
      continue
    }

    const slice = close.slice(index - 19, index + 1)
    const avg = mean(slice)
    const variance = slice.reduce((acc, value) => acc + (value - avg) ** 2, 0) / slice.length
    const std = Math.sqrt(variance)

    sma20.push(avg)
    bbUpper.push(avg + std * 2)
    bbLower.push(avg - std * 2)
  }

  return {
    x: candles.map((candle) => candle.time / 1000),
    open: candles.map((candle) => candle.open),
    high: candles.map((candle) => candle.high),
    low: candles.map((candle) => candle.low),
    close,
    ema: candles.map((candle) => candle.ema),
    sma20,
    bbUpper,
    bbLower,
  }
}

const toAlignedData = (series: CandleArrays, indicators: Indicators): AlignedData => [
  series.x,
  series.close,
  indicators.ema ? series.ema : series.ema.map(() => null),
  indicators.sma ? series.sma20 : series.sma20.map(() => null),
  indicators.bb ? series.bbUpper : series.bbUpper.map(() => null),
  indicators.bb ? series.bbLower : series.bbLower.map(() => null),
]

const createCandlestickPlugin = (seriesRef: MutableRefObject<CandleArrays>): Plugin => ({
  hooks: {
    draw: [
      (plot) => {
        const { x, open, high, low, close } = seriesRef.current
        if (x.length < 2) {
          return
        }

        const ctx = plot.ctx
        const candleWidth = Math.max(3, Math.floor(plot.bbox.width / x.length) - 2)

        for (let index = 0; index < x.length; index += 1) {
          const xPos = Math.round(plot.valToPos(x[index], "x", true))
          const openY = Math.round(plot.valToPos(open[index], "y", true))
          const closeY = Math.round(plot.valToPos(close[index], "y", true))
          const highY = Math.round(plot.valToPos(high[index], "y", true))
          const lowY = Math.round(plot.valToPos(low[index], "y", true))
          const bullish = close[index] >= open[index]
          const color = bullish ? "#22c55e" : "#ef4444"

          ctx.strokeStyle = color
          ctx.beginPath()
          ctx.moveTo(xPos, highY)
          ctx.lineTo(xPos, lowY)
          ctx.stroke()

          ctx.fillStyle = color
          ctx.fillRect(xPos - candleWidth / 2, Math.min(openY, closeY), candleWidth, Math.max(1, Math.abs(closeY - openY)))
        }
      },
    ],
  },
})

export const TradingChartPanel: FC = () => {
  const chartHostRef = useRef<HTMLDivElement>(null)
  const overlayRef = useRef<HTMLCanvasElement>(null)
  const chartRef = useRef<uPlot | null>(null)
  const workerRef = useRef<Worker | null>(null)
  const frameRef = useRef<number | null>(null)
  const candlesRef = useRef<Candle[]>([])
  const seriesRef = useRef<CandleArrays>({ x: [], open: [], high: [], low: [], close: [], ema: [], sma20: [], bbUpper: [], bbLower: [] })
  const hoverPointRef = useRef<ChartPoint | null>(null)
  const indicatorsRef = useRef<Indicators>({ ema: true, sma: false, bb: false })
  const lastToastKeyRef = useRef<string>("")
  const disconnectToastTimerRef = useRef<number | null>(null)

  const { colorScheme } = useMantineColorScheme()
  const { toast } = useToast()
  const isDark = colorScheme === "dark"

  const [symbol, setSymbol] = useState(DEFAULT_CHART_SYMBOL)
  const { currency, setCurrency, currencyOptions } = useCurrencyPreference()
  const { data: symbolOptions = [DEFAULT_CHART_SYMBOL] } = useMarketSymbols(currency)
  const [intervalToken, setIntervalToken] = useState("1S")
  const [intervalModalOpened, setIntervalModalOpened] = useState(false)
  const [intervalDraft, setIntervalDraft] = useState("1S")
  const [tool, setTool] = useState<DrawTool>("none")
  const [indicators, setIndicators] = useState<Indicators>({ ema: true, sma: false, bb: false })
  const [drawings, setDrawings] = useState<Drawing[]>([])
  const [pendingStart, setPendingStart] = useState<ChartPoint | null>(null)
  const [lastPrice, setLastPrice] = useState(100)
  const [ticksPerSecond, setTicksPerSecond] = useState(0)
  const [streamStatus, setStreamStatus] = useState<"connected" | "disconnected" | "error">("disconnected")
  const [streamError, setStreamError] = useState<string | null>(null)
  const [signal, setSignal] = useState<ChartSignal | null>(null)

  useEffect(() => {
    if (!symbolOptions.includes(symbol)) {
      setSymbol(symbolOptions[0] ?? DEFAULT_CHART_SYMBOL)
    }
  }, [symbol, symbolOptions])

  const summary = useMemo(() => {
    const candle = candlesRef.current.at(-1)
    const enabled = [indicators.ema ? "EMA14" : null, indicators.sma ? "SMA20" : null, indicators.bb ? "BB(20,2)" : null].filter(Boolean).join(", ")
    return candle
      ? `O ${formatCurrency(candle.open, currency)} H ${formatCurrency(candle.high, currency)} L ${formatCurrency(candle.low, currency)} C ${formatCurrency(candle.close, currency)} · ${enabled || "No indicators"}`
      : "Waiting for stream…"
  }, [lastPrice, indicators, currency])

  const drawOverlay = () => {
    const plot = chartRef.current
    const canvas = overlayRef.current
    if (!plot || !canvas) {
      return
    }

    const dpr = window.devicePixelRatio || 1
    const width = plot.bbox.width
    const height = plot.bbox.height
    const left = plot.bbox.left
    const top = plot.bbox.top

    canvas.style.left = `${left}px`
    canvas.style.top = `${top}px`
    canvas.style.width = `${width}px`
    canvas.style.height = `${height}px`
    canvas.width = Math.floor(width * dpr)
    canvas.height = Math.floor(height * dpr)

    const ctx = canvas.getContext("2d")
    if (!ctx) {
      return
    }

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.clearRect(0, 0, width, height)
    ctx.strokeStyle = "#38bdf8"
    ctx.lineWidth = 2

    drawings.forEach((drawing) => {
      if (drawing.type === "hline") {
        const y = plot.valToPos(drawing.y, "y", true) - top
        ctx.beginPath()
        ctx.moveTo(0, y)
        ctx.lineTo(width, y)
        ctx.stroke()
        return
      }

      if (drawing.type === "vline") {
        const x = plot.valToPos(drawing.time, "x", true) - left
        ctx.beginPath()
        ctx.moveTo(x, 0)
        ctx.lineTo(x, height)
        ctx.stroke()
        return
      }

      const startX = plot.valToPos(drawing.start.time, "x", true) - left
      const startY = plot.valToPos(drawing.start.price, "y", true) - top
      const endX = plot.valToPos(drawing.end.time, "x", true) - left
      const endY = plot.valToPos(drawing.end.price, "y", true) - top

      if (drawing.type === "ray") {
        const xMax = plot.scales.x.max ?? drawing.end.time
        const rayX = plot.valToPos(xMax, "x", true) - left
        const slope = (endY - startY) / (endX - startX || 1)
        const rayY = startY + slope * (rayX - startX)
        ctx.beginPath()
        ctx.moveTo(startX, startY)
        ctx.lineTo(rayX, rayY)
        ctx.stroke()
        return
      }

      ctx.beginPath()
      ctx.moveTo(startX, startY)
      ctx.lineTo(endX, endY)
      ctx.stroke()
    })

    if (pendingStart) {
      const x = plot.valToPos(pendingStart.time, "x", true) - left
      const y = plot.valToPos(pendingStart.price, "y", true) - top
      ctx.fillStyle = "#38bdf8"
      ctx.beginPath()
      ctx.arc(x, y, 4, 0, Math.PI * 2)
      ctx.fill()
    }

    const hover = hoverPointRef.current
    if (hover) {
      const hoverX = plot.valToPos(hover.time, "x", true) - left
      const hoverY = plot.valToPos(hover.price, "y", true) - top
      const crosshair = isDark ? "rgba(255,255,255,0.5)" : "rgba(0,0,0,0.55)"
      const chipBg = isDark ? "rgba(22, 28, 36, 0.95)" : "rgba(255,255,255,0.95)"
      const chipFg = isDark ? "#e5e7eb" : "#111827"

      ctx.strokeStyle = crosshair
      ctx.lineWidth = 1
      ctx.setLineDash([4, 4])
      ctx.beginPath()
      ctx.moveTo(hoverX, 0)
      ctx.lineTo(hoverX, height)
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(0, hoverY)
      ctx.lineTo(width, hoverY)
      ctx.stroke()
      ctx.setLineDash([])

      const yLabel = formatCurrency(hover.price, currency)
      const xLabel = formatDateTime(hover.time * 1000)

      ctx.font = "11px sans-serif"
      const yLabelWidth = Math.ceil(ctx.measureText(yLabel).width) + 10
      const xLabelWidth = Math.ceil(ctx.measureText(xLabel).width) + 10
      const chipHeight = 18

      ctx.fillStyle = chipBg
      ctx.fillRect(Math.max(0, width - yLabelWidth), Math.min(height - chipHeight, Math.max(0, hoverY - chipHeight / 2)), yLabelWidth, chipHeight)
      ctx.fillStyle = chipFg
      ctx.fillText(yLabel, Math.max(4, width - yLabelWidth + 5), Math.min(height - 5, Math.max(12, hoverY + 4)))

      const xChipX = Math.min(Math.max(0, hoverX - xLabelWidth / 2), Math.max(0, width - xLabelWidth))
      ctx.fillStyle = chipBg
      ctx.fillRect(xChipX, Math.max(0, height - chipHeight), xLabelWidth, chipHeight)
      ctx.fillStyle = chipFg
      ctx.fillText(xLabel, xChipX + 5, Math.max(0, height - 5))
    }
  }

  useEffect(() => {
    const host = chartHostRef.current
    if (!host) {
      return
    }

    const axisStroke = isDark ? "rgba(255,255,255,0.35)" : "rgba(0,0,0,0.35)"
    const gridStroke = isDark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)"

    const options: Options = {
      width: Math.max(host.clientWidth, 320),
      height: chartHeight,
      scales: {
        x: { time: true },
        y: { auto: true },
      },
      axes: [
        { side: 2, stroke: axisStroke, grid: { stroke: gridStroke, width: 1 } },
        { side: 1, stroke: axisStroke, grid: { stroke: gridStroke, width: 1 } },
      ],
      series: [
        { label: "Time" },
        { label: "Close", stroke: "rgba(0,0,0,0)", width: 0 },
        { label: "EMA14", stroke: "#facc15", width: 2 },
        { label: "SMA20", stroke: "#38bdf8", width: 2 },
        { label: "BB Upper", stroke: "#a78bfa", width: 1 },
        { label: "BB Lower", stroke: "#a78bfa", width: 1 },
      ],
      plugins: [
        createCandlestickPlugin(seriesRef),
        {
          hooks: {
            draw: [drawOverlay],
            setScale: [drawOverlay],
            setSize: [drawOverlay],
          },
        },
      ],
    }

    chartRef.current = new uPlot(options, [[], [], [], [], [], []], host)

    if (seriesRef.current.x.length > 0) {
      chartRef.current.setData(toAlignedData(seriesRef.current, indicators), true)
    }

    const resizeObserver = new ResizeObserver(() => {
      if (!chartRef.current) {
        return
      }

      chartRef.current.setSize({
        width: Math.max(host.clientWidth, 320),
        height: chartHeight,
      })
      drawOverlay()
    })

    resizeObserver.observe(host)

    return () => {
      resizeObserver.disconnect()
      if (frameRef.current) {
        cancelAnimationFrame(frameRef.current)
      }
      chartRef.current?.destroy()
      chartRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isDark])

  useEffect(() => {
    const worker = new Worker(new URL("../../workers/chartStreamWorker.ts", import.meta.url), { type: "module" })
    workerRef.current = worker

    worker.onmessage = (event: MessageEvent<WorkerPayload>) => {
      candlesRef.current = event.data.payload.candles
      seriesRef.current = toCandleArrays(event.data.payload.candles)
      setLastPrice(event.data.payload.lastPrice)
      setTicksPerSecond(event.data.payload.ticksPerSecond)
      setStreamStatus(event.data.payload.status)
      setStreamError(event.data.payload.error || null)
      setSignal(event.data.payload.signal || null)

      if (!chartRef.current || frameRef.current) {
        return
      }

      frameRef.current = requestAnimationFrame(() => {
        frameRef.current = null
        chartRef.current?.setData(toAlignedData(seriesRef.current, indicatorsRef.current), true)
        drawOverlay()
      })
    }

    return () => {
      worker.postMessage({ type: "stop" })
      worker.terminate()
      workerRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    indicatorsRef.current = indicators
  }, [indicators])

  useEffect(() => {
    if (!chartRef.current || frameRef.current) {
      return
    }

    frameRef.current = requestAnimationFrame(() => {
      frameRef.current = null
      chartRef.current?.setData(toAlignedData(seriesRef.current, indicatorsRef.current), true)
      drawOverlay()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [indicators])

  useEffect(() => {
    workerRef.current?.postMessage({
      type: "start",
      payload: {
        symbol,
        intervalToken,
        intervalMs: intervalTokenToMs(intervalToken),
        historySize: 500,
      },
    })
  }, [intervalToken, symbol])

  useEffect(() => {
    drawOverlay()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drawings, pendingStart])

  useEffect(() => {
    if (disconnectToastTimerRef.current) {
      window.clearTimeout(disconnectToastTimerRef.current)
      disconnectToastTimerRef.current = null
    }

    if (streamStatus === "error") {
      const message = streamError || "Unknown data stream error"
      const toastKey = `error:${message}`
      if (lastToastKeyRef.current !== toastKey) {
        lastToastKeyRef.current = toastKey
        toast({
          id: "trading-chart-stream-error",
          title: "Market data stream error",
          message,
          variant: "error",
          timestamp: Date.now(),
        })
      }
      return
    }

    if (streamStatus === "disconnected") {
      const message = streamError || "Connection to market data feed was lost."
      const toastKey = `disconnected:${message}`
      disconnectToastTimerRef.current = window.setTimeout(() => {
        if (lastToastKeyRef.current === toastKey) {
          return
        }

        lastToastKeyRef.current = toastKey
        toast({
          id: "trading-chart-stream-disconnected",
          title: "Market data disconnected",
          message,
          variant: "error",
          timestamp: Date.now(),
        })
      }, 20_000)
      return
    }

    if (streamStatus === "connected") {
      lastToastKeyRef.current = "connected"
    }
  }, [streamError, streamStatus, toast])

  useEffect(() => {
    return () => {
      if (disconnectToastTimerRef.current) {
        window.clearTimeout(disconnectToastTimerRef.current)
      }
    }
  }, [])

  const toPointFromMouse = (event: MouseEvent<HTMLCanvasElement>): ChartPoint | null => {
    const plot = chartRef.current
    if (!plot) {
      return null
    }

    const rect = event.currentTarget.getBoundingClientRect()
    const x = event.clientX - rect.left
    const y = event.clientY - rect.top

    // posToVal(..., can=false) expects plot-space css pixels relative to uPlot root,
    // so include plot bbox offsets instead of raw overlay-local coordinates.
    const plotX = x + plot.bbox.left
    const plotY = y + plot.bbox.top

    return {
      time: plot.posToVal(plotX, "x", false),
      price: plot.posToVal(plotY, "y", false),
    }
  }

  const handleOverlayMouseMove = (event: MouseEvent<HTMLCanvasElement>) => {
    const point = toPointFromMouse(event)
    if (!point) {
      return
    }

    hoverPointRef.current = point
    drawOverlay()
  }

  const handleOverlayMouseLeave = () => {
    hoverPointRef.current = null
    drawOverlay()
  }

  const handleOverlayClick = (event: MouseEvent<HTMLCanvasElement>) => {
    const point = toPointFromMouse(event)
    if (!point || tool === "none") {
      return
    }

    if (tool === "hline") {
      setDrawings((prev) => [...prev, { id: crypto.randomUUID(), type: "hline", y: point.price }])
      return
    }

    if (tool === "vline") {
      setDrawings((prev) => [...prev, { id: crypto.randomUUID(), type: "vline", time: point.time }])
      return
    }

    if (!pendingStart) {
      setPendingStart(point)
      return
    }

    const start = pendingStart.time <= point.time ? pendingStart : point
    const end = pendingStart.time <= point.time ? point : pendingStart

    setDrawings((prev) => [...prev, { id: crypto.randomUUID(), type: tool === "ray" ? "ray" : "trendline", start, end }])
    setPendingStart(null)
  }

  const toggleIndicator = (key: keyof Indicators) => {
    setIndicators((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  return (
    <Stack gap="sm">
      <Group className={classes.toolbar} justify="space-between">
        <Group>
          <Select value={currency} onChange={(value) => setCurrency(value ?? currency)} data={currencyOptions} w={95} size="xs" aria-label="Quote currency" />
          <Select
            value={symbol}
            onChange={(value) => setSymbol((value as string) || DEFAULT_CHART_SYMBOL)}
            data={symbolOptions}
            w={130}
            size="xs"
            aria-label="Chart symbol"
          />
          <Button
            size="xs"
            variant="light"
            aria-label="Chart frequency"
            onClick={() => {
              setIntervalDraft(intervalToken)
              setIntervalModalOpened(true)
            }}>
            Interval {intervalToken}
          </Button>
          <SegmentedControl
            size="xs"
            value={tool}
            onChange={(value) => {
              setTool(value as DrawTool)
              setPendingStart(null)
            }}
            data={[
              { value: "none", label: "Pan" },
              { value: "trendline", label: "Trendline" },
              { value: "ray", label: "Ray" },
              { value: "hline", label: "H-Line" },
              { value: "vline", label: "V-Line" },
            ]}
          />
          <Button size="xs" variant={indicators.ema ? "filled" : "light"} onClick={() => toggleIndicator("ema")}>
            EMA
          </Button>
          <Button size="xs" variant={indicators.sma ? "filled" : "light"} onClick={() => toggleIndicator("sma")}>
            SMA
          </Button>
          <Button size="xs" variant={indicators.bb ? "filled" : "light"} onClick={() => toggleIndicator("bb")}>
            BB
          </Button>
          <Button size="xs" variant="light" onClick={() => setDrawings([])}>
            Clear drawings
          </Button>
        </Group>

        <Group gap="xs">
          <Badge color={streamStatus === "connected" ? "green" : streamStatus === "error" ? "red" : "gray"} variant="light">
            {streamStatus === "connected" ? `${ticksPerSecond} ticks/s` : streamStatus}
          </Badge>
          <Badge color="blue" variant="light">{`${symbol} ${formatCurrency(lastPrice, currency)}`}</Badge>
          <Badge color={signal?.side === "BUY" ? "green" : signal?.side === "SELL" ? "red" : "gray"} variant="filled">
            {signal ? `${signal.side} ${(signal.confidence * 100).toFixed(0)}%` : "HOLD"}
          </Badge>
        </Group>
      </Group>

      <Paper className={classes.wrapper}>
        <div className={classes.legend}>
          <Text size="xs" c="dimmed">
            {streamError ? `${summary} · ${streamError}` : `${summary}${signal ? ` · Signal ${signal.side} (${(signal.confidence * 100).toFixed(0)}%)` : ""}`}
          </Text>
        </div>
        <div ref={chartHostRef} className={classes.plotHost} />
        <canvas
          ref={overlayRef}
          className={classes.overlayCanvas}
          onClick={handleOverlayClick}
          onMouseMove={handleOverlayMouseMove}
          onMouseLeave={handleOverlayMouseLeave}
        />
      </Paper>
      <Modal opened={intervalModalOpened} onClose={() => setIntervalModalOpened(false)} title="Set chart interval" centered>
        <Stack>
          <Text size="sm" c="dimmed">
            Use format number + unit: S (seconds), M (minutes), H (hours), D (days), MO (months), Y (years). Example: 15D.
          </Text>
          <TextInput
            value={intervalDraft}
            onChange={(event) => setIntervalDraft(event.currentTarget.value.toUpperCase())}
            placeholder="15D"
            aria-label="Custom interval"
          />
          <Group gap="xs">
            {intervalPresets.map((preset) => (
              <Button key={preset} size="xs" variant="subtle" onClick={() => setIntervalDraft(preset)}>
                {preset}
              </Button>
            ))}
          </Group>
          <Group justify="flex-end">
            <Button size="xs" variant="subtle" onClick={() => setIntervalModalOpened(false)}>
              Cancel
            </Button>
            <Button
              size="xs"
              onClick={() => {
                const normalized = normalizeIntervalToken(intervalDraft)
                setIntervalToken(normalized)
                setIntervalModalOpened(false)
              }}>
              Apply
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}
