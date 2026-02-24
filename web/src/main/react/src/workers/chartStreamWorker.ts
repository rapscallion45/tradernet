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
}

type WorkerMessage =
  | { type: "start"; payload: StreamConfig }
  | { type: "stop" }

let tickTimer: number | undefined
let emitTimer: number | undefined
let current: Candle | null = null
let candles: Candle[] = []
let lastPrice = 100
let emaPrev = 100
let intervalMs = 1000
let historySize = 240
let symbol = "BTCUSD"
const emaPeriod = 14
const alpha = 2 / (emaPeriod + 1)

const tick = () => {
  const now = Date.now()
  const nextPrice = Math.max(1, lastPrice + (Math.random() - 0.5) * 0.8)
  if (!current) {
    current = {
      time: Math.floor(now / intervalMs) * intervalMs,
      open: lastPrice,
      high: Math.max(lastPrice, nextPrice),
      low: Math.min(lastPrice, nextPrice),
      close: nextPrice,
      volume: 1,
      ema: emaPrev,
    }
    lastPrice = nextPrice
    return
  }

  const bucket = Math.floor(now / intervalMs) * intervalMs
  if (bucket !== current.time) {
    emaPrev = alpha * current.close + (1 - alpha) * emaPrev
    current.ema = emaPrev
    candles.push(current)
    if (candles.length > historySize) {
      candles = candles.slice(candles.length - historySize)
    }

    current = {
      time: bucket,
      open: lastPrice,
      high: Math.max(lastPrice, nextPrice),
      low: Math.min(lastPrice, nextPrice),
      close: nextPrice,
      volume: 1,
      ema: emaPrev,
    }
  } else {
    current.high = Math.max(current.high, nextPrice)
    current.low = Math.min(current.low, nextPrice)
    current.close = nextPrice
    current.volume += 1
  }

  lastPrice = nextPrice
}

const emit = () => {
  if (!current) {
    return
  }

  self.postMessage({
    type: "bars",
    payload: {
      symbol,
      candles: [...candles, current],
      lastPrice,
      ticksPerSecond: 100,
    },
  })
}

const stop = () => {
  if (tickTimer) {
    self.clearInterval(tickTimer)
  }
  if (emitTimer) {
    self.clearInterval(emitTimer)
  }
  tickTimer = undefined
  emitTimer = undefined
}

self.onmessage = (event: MessageEvent<WorkerMessage>) => {
  if (event.data.type === "stop") {
    stop()
    return
  }

  stop()

  const payload = event.data.payload
  symbol = payload.symbol
  intervalMs = payload.intervalMs
  historySize = payload.historySize
  lastPrice = payload.seedPrice
  emaPrev = payload.seedPrice
  candles = []
  current = null

  tickTimer = self.setInterval(tick, 10)
  emitTimer = self.setInterval(emit, 33)
}
