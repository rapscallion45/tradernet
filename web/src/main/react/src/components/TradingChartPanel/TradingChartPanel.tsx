import { FC, MouseEvent, useEffect, useMemo, useRef, useState } from "react"
import { Badge, Button, Group, Paper, SegmentedControl, Select, Stack, Text } from "@mantine/core"
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

type Point = { x: number; y: number }

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
  }
}

const chartHeight = 420
const padding = { top: 20, right: 56, bottom: 24, left: 18 }

const toPrice = (y: number, min: number, max: number) => {
  const innerHeight = chartHeight - padding.top - padding.bottom
  return max - ((y - padding.top) / innerHeight) * (max - min)
}

export const TradingChartPanel: FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const workerRef = useRef<Worker | null>(null)
  const frameRequestedRef = useRef(false)
  const latestBarsRef = useRef<Candle[]>([])
  const symbolRef = useRef("BTCUSD")

  const [symbol, setSymbol] = useState("BTCUSD")
  const [intervalMs, setIntervalMs] = useState("1000")
  const [tool, setTool] = useState<DrawTool>("none")
  const [drawings, setDrawings] = useState<Drawing[]>([])
  const [pendingStart, setPendingStart] = useState<Point | null>(null)
  const [lastPrice, setLastPrice] = useState(100)
  const [ticksPerSecond, setTicksPerSecond] = useState(0)

  const latestSummary = useMemo(() => {
    const last = latestBarsRef.current.at(-1)
    return last
      ? `O ${last.open.toFixed(2)} H ${last.high.toFixed(2)} L ${last.low.toFixed(2)} C ${last.close.toFixed(2)} · EMA14 ${last.ema.toFixed(2)}`
      : "Waiting for stream…"
  }, [lastPrice, symbol])

  const requestDraw = () => {
    if (frameRequestedRef.current) {
      return
    }

    frameRequestedRef.current = true
    requestAnimationFrame(() => {
      frameRequestedRef.current = false
      const canvas = canvasRef.current
      if (!canvas) {
        return
      }

      const ctx = canvas.getContext("2d")
      if (!ctx) {
        return
      }

      const width = canvas.clientWidth
      const height = chartHeight
      const dpr = window.devicePixelRatio || 1
      canvas.width = Math.floor(width * dpr)
      canvas.height = Math.floor(height * dpr)
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

      ctx.fillStyle = "#0a0d14"
      ctx.fillRect(0, 0, width, height)

      const bars = latestBarsRef.current
      if (bars.length < 2) {
        return
      }

      const min = Math.min(...bars.map((b) => Math.min(b.low, b.ema)))
      const max = Math.max(...bars.map((b) => Math.max(b.high, b.ema)))
      const innerHeight = height - padding.top - padding.bottom
      const innerWidth = width - padding.left - padding.right
      const step = innerWidth / bars.length
      const candleBody = Math.max(2, step * 0.6)

      const y = (price: number) => padding.top + ((max - price) / (max - min || 1)) * innerHeight

      ctx.strokeStyle = "rgba(255,255,255,0.09)"
      ctx.lineWidth = 1
      for (let i = 0; i < 5; i += 1) {
        const gridY = padding.top + (innerHeight / 4) * i
        ctx.beginPath()
        ctx.moveTo(padding.left, gridY)
        ctx.lineTo(width - padding.right, gridY)
        ctx.stroke()
      }

      bars.forEach((bar, index) => {
        const xCenter = padding.left + step * (index + 0.5)
        const wickColor = bar.close >= bar.open ? "#4ade80" : "#f87171"
        const bodyTop = Math.min(y(bar.open), y(bar.close))
        const bodyBottom = Math.max(y(bar.open), y(bar.close))

        ctx.strokeStyle = wickColor
        ctx.beginPath()
        ctx.moveTo(xCenter, y(bar.high))
        ctx.lineTo(xCenter, y(bar.low))
        ctx.stroke()

        ctx.fillStyle = wickColor
        ctx.fillRect(xCenter - candleBody / 2, bodyTop, candleBody, Math.max(1, bodyBottom - bodyTop))
      })

      ctx.strokeStyle = "#facc15"
      ctx.lineWidth = 2
      ctx.beginPath()
      bars.forEach((bar, index) => {
        const xCenter = padding.left + step * (index + 0.5)
        const yPos = y(bar.ema)
        if (index === 0) {
          ctx.moveTo(xCenter, yPos)
        } else {
          ctx.lineTo(xCenter, yPos)
        }
      })
      ctx.stroke()

      drawings.forEach((drawing) => {
        ctx.lineWidth = 2
        ctx.strokeStyle = "#38bdf8"
        if (drawing.type === "hline") {
          const yPos = y(drawing.y)
          ctx.beginPath()
          ctx.moveTo(padding.left, yPos)
          ctx.lineTo(width - padding.right, yPos)
          ctx.stroke()
        } else {
          ctx.beginPath()
          ctx.moveTo(drawing.start.x, drawing.start.y)
          ctx.lineTo(drawing.end.x, drawing.end.y)
          ctx.stroke()
        }
      })

      if (pendingStart) {
        ctx.fillStyle = "#38bdf8"
        ctx.beginPath()
        ctx.arc(pendingStart.x, pendingStart.y, 4, 0, Math.PI * 2)
        ctx.fill()
      }
    })
  }

  useEffect(() => {
    const worker = new Worker(new URL("../../workers/chartStreamWorker.ts", import.meta.url), { type: "module" })
    workerRef.current = worker

    worker.onmessage = (event: MessageEvent<WorkerPayload>) => {
      latestBarsRef.current = event.data.payload.candles
      symbolRef.current = event.data.payload.symbol
      setLastPrice(event.data.payload.lastPrice)
      setTicksPerSecond(event.data.payload.ticksPerSecond)
      requestDraw()
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
      },
    })
  }, [intervalMs, symbol])

  useEffect(() => {
    requestDraw()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drawings, pendingStart])

  const handleCanvasClick = (event: MouseEvent<HTMLCanvasElement>) => {
    if (tool === "none") {
      return
    }

    const rect = event.currentTarget.getBoundingClientRect()
    const point = { x: event.clientX - rect.left, y: event.clientY - rect.top }

    const bars = latestBarsRef.current
    if (bars.length < 2) {
      return
    }

    const min = Math.min(...bars.map((b) => Math.min(b.low, b.ema)))
    const max = Math.max(...bars.map((b) => Math.max(b.high, b.ema)))

    if (tool === "hline") {
      setDrawings((existing) => [...existing, { id: crypto.randomUUID(), type: "hline", y: toPrice(point.y, min, max) }])
      return
    }

    if (!pendingStart) {
      setPendingStart(point)
      return
    }

    setDrawings((existing) => [...existing, { id: crypto.randomUUID(), type: "trendline", start: pendingStart, end: point }])
    setPendingStart(null)
  }

  return (
    <Stack gap="sm">
      <Group className={classes.toolbar} justify="space-between">
        <Group>
          <Select
            label="Symbol"
            value={symbol}
            onChange={(value) => setSymbol(value || "BTCUSD")}
            data={["BTCUSD", "ETHUSD", "AAPL", "TSLA"]}
            w={130}
            size="xs"
          />
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
          <Badge color="green" variant="light">{`${ticksPerSecond} ticks/s`}</Badge>
          <Badge color="blue" variant="light">{`${symbolRef.current} ${lastPrice.toFixed(2)}`}</Badge>
        </Group>
      </Group>

      <Paper className={classes.wrapper}>
        <div className={classes.legend}>
          <Text size="xs" c="dimmed">
            {latestSummary}
          </Text>
        </div>
        <canvas ref={canvasRef} className={classes.canvas} onClick={handleCanvasClick} />
      </Paper>
    </Stack>
  )
}
