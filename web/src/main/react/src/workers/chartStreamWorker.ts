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
  intervalMs: number
  seedPrice: number
  historySize: number
  apiKey?: string
}

type WorkerMessage = { type: "start"; payload: StreamConfig } | { type: "stop" }

let socket: WebSocket | null = null
let emitTimer: number | undefined
let current: Candle | null = null
let candles: Candle[] = []
let lastPrice = 100
let emaPrev = 100
let intervalMs = 1000
let historySize = 240
let symbol = "AAPL"
let tickCount = 0

const emaPeriod = 14
const alpha = 2 / (emaPeriod + 1)

const closeCurrentBar = (nextBucket: number, nextPrice: number, nextVolume: number) => {
  if (!current) {
    current = {
      time: nextBucket,
      open: nextPrice,
      high: nextPrice,
      low: nextPrice,
      close: nextPrice,
      volume: nextVolume,
      ema: emaPrev,
    }
    return
  }

  emaPrev = alpha * current.close + (1 - alpha) * emaPrev
  current.ema = emaPrev
  candles.push(current)

  if (candles.length > historySize) {
    candles = candles.slice(candles.length - historySize)
  }

  current = {
    time: nextBucket,
    open: lastPrice,
    high: nextPrice,
    low: nextPrice,
    close: nextPrice,
    volume: nextVolume,
    ema: emaPrev,
  }
}

const ingestTrade = (price: number, volume = 1, timestamp = Date.now()) => {
  const bucket = Math.floor(timestamp / intervalMs) * intervalMs

  if (!current) {
    current = {
      time: bucket,
      open: price,
      high: price,
      low: price,
      close: price,
      volume,
      ema: emaPrev,
    }
    lastPrice = price
    tickCount += 1
    return
  }

  if (bucket !== current.time) {
    closeCurrentBar(bucket, price, volume)
  } else {
    current.high = Math.max(current.high, price)
    current.low = Math.min(current.low, price)
    current.close = price
    current.volume += volume
  }

  lastPrice = price
  tickCount += 1
}

const emit = (status: "connected" | "disconnected" | "error" = "connected", error?: string) => {
  self.postMessage({
    type: "bars",
    payload: {
      symbol,
      candles: current ? [...candles, current] : candles,
      lastPrice,
      ticksPerSecond: tickCount,
      status,
      error,
    },
  })

  tickCount = 0
}

const mapSymbolForFinnhub = (selectedSymbol: string): string => {
  if (selectedSymbol === "BTCUSD") {
    return "BINANCE:BTCUSDT"
  }
  if (selectedSymbol === "ETHUSD") {
    return "BINANCE:ETHUSDT"
  }
  return selectedSymbol
}

const stop = () => {
  if (emitTimer) {
    self.clearInterval(emitTimer)
  }
  emitTimer = undefined

  if (socket) {
    socket.close()
    socket = null
  }
}

const start = ({ symbol: selectedSymbol, intervalMs: nextIntervalMs, seedPrice, historySize: nextHistorySize, apiKey }: StreamConfig) => {
  stop()

  symbol = selectedSymbol
  intervalMs = nextIntervalMs
  historySize = nextHistorySize
  lastPrice = seedPrice
  emaPrev = seedPrice
  current = null
  candles = []
  tickCount = 0

  if (!apiKey) {
    self.postMessage({
      type: "bars",
      payload: {
        symbol,
        candles: [],
        lastPrice,
        ticksPerSecond: 0,
        status: "error",
        error: "Missing VITE_FINNHUB_API_KEY",
      },
    })
    return
  }

  const finnhubSymbol = mapSymbolForFinnhub(selectedSymbol)
  socket = new WebSocket(`wss://ws.finnhub.io?token=${apiKey}`)

  socket.onopen = () => {
    socket?.send(JSON.stringify({ type: "subscribe", symbol: finnhubSymbol }))
    emit("connected")
  }

  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data as string) as {
        type?: string
        data?: Array<{ p: number; v?: number; t: number }>
      }

      if (message.type !== "trade" || !message.data?.length) {
        return
      }

      for (const trade of message.data) {
        if (!Number.isFinite(trade.p)) {
          continue
        }

        ingestTrade(trade.p, trade.v ?? 1, trade.t)
      }
    } catch {
      emit("error", "Failed parsing Finnhub payload")
    }
  }

  socket.onerror = () => {
    emit("error", "Finnhub socket error")
  }

  socket.onclose = () => {
    emit("disconnected")
  }

  emitTimer = self.setInterval(() => {
    emit(socket?.readyState === WebSocket.OPEN ? "connected" : "disconnected")
  }, 1000)
}

self.onmessage = (event: MessageEvent<WorkerMessage>) => {
  if (event.data.type === "stop") {
    stop()
    return
  }

  start(event.data.payload)
}
