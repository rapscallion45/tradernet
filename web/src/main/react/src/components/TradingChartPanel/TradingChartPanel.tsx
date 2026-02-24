import { FC, MouseEvent, MutableRefObject, useEffect, useMemo, useRef, useState } from "react"
import { Badge, Button, Group, Paper, SegmentedControl, Select, Stack, Text } from "@mantine/core"
import uPlot, { AlignedData, Options, Plugin } from "uplot"
import "uplot/dist/uPlot.min.css"
import classes from "./TradingChartPanel.module.css"

type Candle = {
  time: number
  open: number
  high: number
  low: number
  close: number
  ema: number
}

type DrawTool = "none" | "trendline" | "hline"

type Point = {
  x: number
  y: number
}

type Drawing =
  | { id: string; type: "hline"; y: number }
  | { id: string; type: "trendline"; start: Point; end: Point }

type WorkerPayload = {
  type: "bars"
  payload: {
    symbol: string
    candles: Candle[]
    lastPrice: number
    ticksPerSecond: number
    status: "connected" | "disconnected" | "error"
    error?: string
  }
}

type CandleArrays = {
  x: number[]
  open: number[]
  high: number[]
  low: number[]
  close: number[]
  ema: number[]
}

const chartHeight = 420

const toCandleArrays = (candles: Candle[]): CandleArrays => ({
  x: candles.map((candle) => candle.time / 1000),
  open: candles.map((candle) => candle.open),
  high: candles.map((candle) => candle.high),
  low: candles.map((candle) => candle.low),
  close: candles.map((candle) => candle.close),
  ema: candles.map((candle) => candle.ema),
})

const toAlignedData = (series: CandleArrays): AlignedData => [series.x, series.close, series.ema]

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
  const seriesRef = useRef<CandleArrays>({ x: [], open: [], high: [], low: [], close: [], ema: [] })

  const [symbol, setSymbol] = useState("BTCUSD")
  const [intervalMs, setIntervalMs] = useState("1000")
  const [tool, setTool] = useState<DrawTool>("none")
  const [drawings, setDrawings] = useState<Drawing[]>([])
  const [pendingStart, setPendingStart] = useState<Point | null>(null)
  const [lastPrice, setLastPrice] = useState(100)
  const [ticksPerSecond, setTicksPerSecond] = useState(0)
  const [streamStatus, setStreamStatus] = useState<"connected" | "disconnected" | "error">("disconnected")
  const [streamError, setStreamError] = useState<string | null>(null)

  const summary = useMemo(() => {
    const candle = candlesRef.current.at(-1)
    return candle
      ? `O ${candle.open.toFixed(2)} H ${candle.high.toFixed(2)} L ${candle.low.toFixed(2)} C ${candle.close.toFixed(2)} · EMA14 ${candle.ema.toFixed(2)}`
      : "Waiting for stream…"
  }, [lastPrice])

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

      ctx.beginPath()
      ctx.moveTo(drawing.start.x - left, drawing.start.y - top)
      ctx.lineTo(drawing.end.x - left, drawing.end.y - top)
      ctx.stroke()
    })

    if (pendingStart) {
      ctx.fillStyle = "#38bdf8"
      ctx.beginPath()
      ctx.arc(pendingStart.x - left, pendingStart.y - top, 4, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  useEffect(() => {
    const host = chartHostRef.current
    if (!host) {
      return
    }

    const options: Options = {
      width: Math.max(host.clientWidth, 320),
      height: chartHeight,
      scales: {
        x: { time: true },
        y: { auto: true },
      },
      axes: [
        { stroke: "rgba(255,255,255,0.25)", grid: { stroke: "rgba(255,255,255,0.08)" } },
        { stroke: "rgba(255,255,255,0.25)", grid: { stroke: "rgba(255,255,255,0.08)" } },
      ],
      series: [
        { label: "Time" },
        { label: "Close", stroke: "rgba(0,0,0,0)", width: 0 },
        { label: "EMA14", stroke: "#facc15", width: 2 },
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

    chartRef.current = new uPlot(options, [[], [], []], host)

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
  }, [])

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

      if (!chartRef.current || frameRef.current) {
        return
      }

      frameRef.current = requestAnimationFrame(() => {
        frameRef.current = null
        chartRef.current?.setData(toAlignedData(seriesRef.current), true)
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
    workerRef.current?.postMessage({
      type: "start",
      payload: {
        symbol,
        intervalMs: Number(intervalMs),
        seedPrice: lastPrice,
        historySize: 240,
        apiKey: import.meta.env.VITE_FINNHUB_API_KEY,
      },
    })
  }, [symbol, intervalMs])

  useEffect(() => {
    drawOverlay()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drawings, pendingStart])

  const handleOverlayClick = (event: MouseEvent<HTMLCanvasElement>) => {
    const plot = chartRef.current
    if (!plot || tool === "none") {
      return
    }

    const rect = event.currentTarget.getBoundingClientRect()
    const point = {
      x: event.clientX - rect.left + plot.bbox.left,
      y: event.clientY - rect.top + plot.bbox.top,
    }

    if (tool === "hline") {
      setDrawings((prev) => [...prev, { id: crypto.randomUUID(), type: "hline", y: plot.posToVal(point.y, "y") }])
      return
    }

    if (!pendingStart) {
      setPendingStart(point)
      return
    }

    setDrawings((prev) => [...prev, { id: crypto.randomUUID(), type: "trendline", start: pendingStart, end: point }])
    setPendingStart(null)
  }

  return (
    <Stack gap="sm">
      <Group className={classes.toolbar} justify="space-between">
        <Group>
          <Select label="Symbol" value={symbol} onChange={(value) => setSymbol(value || "BTCUSD")} data={["BTCUSD", "ETHUSD", "AAPL", "TSLA"]} w={130} size="xs" />
          <Select
            label="Candle"
            value={intervalMs}
            onChange={(value) => setIntervalMs(value || "1000")}
            data={[
              { value: "1000", label: "1s" },
              { value: "5000", label: "5s" },
            ]}
            w={110}
            size="xs"
          />
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
              { value: "hline", label: "Horizontal" },
            ]}
          />
          <Button size="xs" variant="light" onClick={() => setDrawings([])}>
            Clear drawings
          </Button>
        </Group>

        <Group gap="xs">
          <Badge color={streamStatus === "connected" ? "green" : streamStatus === "error" ? "red" : "gray"} variant="light">
            {streamStatus === "connected" ? `${ticksPerSecond} ticks/s` : streamStatus}
          </Badge>
          <Badge color="blue" variant="light">{`${symbol} ${lastPrice.toFixed(2)}`}</Badge>
        </Group>
      </Group>

      <Paper className={classes.wrapper}>
        <div className={classes.legend}>
          <Text size="xs" c="dimmed">
            {streamError ? `${summary} · ${streamError}` : summary}
          </Text>
        </div>
        <div ref={chartHostRef} className={classes.plotHost} />
        <canvas ref={overlayRef} className={classes.overlayCanvas} onClick={handleOverlayClick} />
      </Paper>
    </Stack>
  )
}
