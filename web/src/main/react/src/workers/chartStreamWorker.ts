type Candle = {
  time: number
  open: number
  high: number
  low: number
  close: number
  volume: number
  ema: number
}

type StreamConfig = {
  symbol: string
  historySize: number
}

type WorkerMessage = { type: "start"; payload: StreamConfig } | { type: "stop" }

type StreamStatus = "connected" | "disconnected" | "error"
type SignalSide = "BUY" | "SELL" | "HOLD"

type MarketBar = {
  symbol: string
  bucketStart: number
  open: number
  high: number
  low: number
  close: number
  volume: number
  closed: boolean
}

type AiSignal = {
  symbol: string
  eventTime: number
  side: SignalSide
  confidence: number
  modelVersion: string
  notes: string[]
}

type WsEnvelope =
  | { type: "bar"; payload: MarketBar }
  | { type: "signal"; payload: AiSignal }
  | { type: string; payload?: unknown }

let emitTimer: number | undefined
let socket: WebSocket | null = null
let candles: Candle[] = []
let historySize = 500
let symbol = "BTCUSDT"
let tickCount = 0
let streamSession = 0
let lastDataAt = 0
let streamStatus: StreamStatus = "disconnected"
let streamError: string | undefined
let lastSignal: AiSignal | null = null

const emaPeriod = 14
const alpha = 2 / (emaPeriod + 1)
const noDataTimeoutMs = 20_000

const normalizeSymbol = (value: string): string => {
  const upper = value.toUpperCase()
  if (upper === "BTCUSD") {
    return "BTCUSDT"
  }
  if (upper === "ETHUSD") {
    return "ETHUSDT"
  }
  return upper
}

const toCandle = (bar: MarketBar): Candle => ({
  time: bar.bucketStart,
  open: bar.open,
  high: bar.high,
  low: bar.low,
  close: bar.close,
  volume: bar.volume,
  ema: bar.close,
})

const recomputeEma = () => {
  let emaPrev = candles[0]?.close ?? 0
  candles = candles.map((candle, index) => {
    if (index === 0) {
      return { ...candle, ema: emaPrev }
    }
    emaPrev = alpha * candle.close + (1 - alpha) * emaPrev
    return { ...candle, ema: emaPrev }
  })
}

const upsertBar = (bar: MarketBar) => {
  const incoming = toCandle(bar)
  const existingIndex = candles.findIndex((candle) => candle.time === incoming.time)

  if (existingIndex >= 0) {
    candles[existingIndex] = incoming
  } else {
    candles.push(incoming)
    candles.sort((left, right) => left.time - right.time)
  }

  if (candles.length > historySize) {
    candles = candles.slice(candles.length - historySize)
  }

  recomputeEma()
  lastDataAt = Date.now()
  tickCount += 1
}

const emit = () => {
  self.postMessage({
    type: "bars",
    payload: {
      symbol,
      candles,
      lastPrice: candles.at(-1)?.close ?? 0,
      ticksPerSecond: tickCount,
      status: streamStatus,
      error: streamError,
      signal: lastSignal,
    },
  })

  tickCount = 0
}

const resolveApiBase = () => {
  const base = (import.meta.env.VITE_SERVER_BASE_URL || "/api").replace(/\/$/, "")
  return base || "/api"
}

const resolveWebsocketUrl = () => {
  const apiBase = resolveApiBase()
  const wsPath = `${apiBase}/ws/market`

  if (wsPath.startsWith("http://") || wsPath.startsWith("https://")) {
    const url = new URL(wsPath)
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:"
    return url.toString()
  }

  const origin = self.location.origin
  const wsProtocol = origin.startsWith("https") ? "wss" : "ws"
  return `${wsProtocol}://${self.location.host}${wsPath}`
}

const preload = async (sessionId: number) => {
  const apiBase = resolveApiBase()
  const [barsResponse, signalResponse] = await Promise.all([
    fetch(`${apiBase}/market/bars?limit=${historySize}`),
    fetch(`${apiBase}/market/signals?limit=1`),
  ])

  if (sessionId !== streamSession) {
    return
  }

  if (!barsResponse.ok) {
    throw new Error(`Unable to load chart bars (${barsResponse.status})`)
  }

  const barsPayload = (await barsResponse.json()) as MarketBar[]
  candles = barsPayload
    .filter((bar) => !bar.symbol || bar.symbol.toUpperCase() === symbol)
    .map(toCandle)
    .sort((left, right) => left.time - right.time)
    .slice(-historySize)

  if (signalResponse.ok) {
    const signals = (await signalResponse.json()) as AiSignal[]
    lastSignal = signals.at(-1) || null
  }

  recomputeEma()
  streamStatus = "connected"
  streamError = undefined
  lastDataAt = Date.now()
}

const connectSocket = (sessionId: number) => {
  const wsUrl = resolveWebsocketUrl()
  socket = new WebSocket(wsUrl)

  socket.onopen = () => {
    if (sessionId !== streamSession) {
      socket?.close()
      return
    }
    streamStatus = "connected"
    streamError = undefined
    lastDataAt = Date.now()
    emit()
  }

  socket.onmessage = (event) => {
    if (sessionId !== streamSession) {
      return
    }

    const envelope = JSON.parse(event.data) as WsEnvelope
    if (envelope.type === "bar" && envelope.payload) {
      const bar = envelope.payload as MarketBar
      if (!bar.symbol || bar.symbol.toUpperCase() === symbol) {
        upsertBar(bar)
      }
    }

    if (envelope.type === "signal" && envelope.payload) {
      const signal = envelope.payload as AiSignal
      if (!signal.symbol || signal.symbol.toUpperCase() === symbol) {
        lastSignal = signal
        lastDataAt = Date.now()
      }
    }

    streamStatus = "connected"
    streamError = undefined
    emit()
  }

  socket.onerror = () => {
    if (sessionId !== streamSession) {
      return
    }
    streamStatus = "error"
    streamError = "Market websocket error"
    emit()
  }

  socket.onclose = () => {
    if (sessionId !== streamSession) {
      return
    }
    streamStatus = "disconnected"
    streamError = "Market websocket disconnected"
    emit()
  }
}

const stop = () => {
  streamSession += 1

  if (emitTimer) {
    self.clearInterval(emitTimer)
  }
  emitTimer = undefined

  if (socket) {
    socket.close()
  }
  socket = null

  streamStatus = "disconnected"
}

const start = async ({ symbol: selectedSymbol, historySize: nextHistorySize }: StreamConfig) => {
  stop()

  const sessionId = streamSession
  symbol = normalizeSymbol(selectedSymbol)
  historySize = nextHistorySize
  candles = []
  tickCount = 0
  lastSignal = null
  streamStatus = "disconnected"
  streamError = undefined
  lastDataAt = Date.now()

  try {
    await preload(sessionId)
  } catch (error) {
    if (sessionId === streamSession) {
      streamStatus = "error"
      streamError = error instanceof Error ? error.message : "Unable to preload market data"
      emit()
    }
    return
  }

  if (sessionId !== streamSession) {
    return
  }

  connectSocket(sessionId)

  emitTimer = self.setInterval(() => {
    if (sessionId !== streamSession) {
      return
    }

    if (Date.now() - lastDataAt > noDataTimeoutMs) {
      streamStatus = "disconnected"
      streamError = "No market data received in 20 seconds"
    }

    emit()
  }, 1_000)

  emit()
}

self.onmessage = (event: MessageEvent<WorkerMessage>) => {
  if (event.data.type === "stop") {
    stop()
    return
  }

  void start(event.data.payload)
}
