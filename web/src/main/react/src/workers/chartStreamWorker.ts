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

type StreamStatus = "connected" | "disconnected" | "error"

type AlphavantageSeriesEntry = {
  "1. open": string
  "2. high": string
  "3. low": string
  "4. close": string
  "5. volume"?: string
}

let emitTimer: number | undefined
let pollTimer: number | undefined
let current: Candle | null = null
let candles: Candle[] = []
let lastPrice = 100
let emaPrev = 100
let intervalMs = 1000
let historySize = 240
let symbol = "AAPL"
let tickCount = 0
let streamSession = 0
let lastDataAt = 0

const emaPeriod = 14
const alpha = 2 / (emaPeriod + 1)
const noDataTimeoutMs = 20_000
const pollIntervalMs = 15_000

const emit = (status: StreamStatus = "connected", error?: string) => {
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

const parseCryptoPair = (selectedSymbol: string): { base: string; quote: string } | null => {
  if (selectedSymbol === "BTCUSD") {
    return { base: "BTC", quote: "USD" }
  }
  if (selectedSymbol === "ETHUSD") {
    return { base: "ETH", quote: "USD" }
  }
  return null
}

const mapResolutionToMinutes = (value: number): number => {
  if (value <= 60_000) {
    return 1
  }
  if (value <= 5 * 60_000) {
    return 5
  }
  if (value <= 15 * 60_000) {
    return 15
  }
  if (value <= 30 * 60_000) {
    return 30
  }
  if (value <= 60 * 60_000) {
    return 60
  }
  return 60
}

const parseAlphaSeries = (payload: Record<string, unknown>): Record<string, AlphavantageSeriesEntry> | null => {
  if (typeof payload.Note === "string") {
    throw new Error(payload.Note)
  }

  if (typeof payload["Error Message"] === "string") {
    throw new Error(payload["Error Message"] as string)
  }

  for (const [key, value] of Object.entries(payload)) {
    if (key.startsWith("Time Series") && value && typeof value === "object") {
      return value as Record<string, AlphavantageSeriesEntry>
    }
  }

  return null
}

const pushClosedCandle = (candle: Candle) => {
  candles.push(candle)
  if (candles.length > historySize) {
    candles = candles.slice(candles.length - historySize)
  }
}

const rolloverToNextBucket = (bucket: number, open: number, high: number, low: number, close: number, volume: number) => {
  if (!current) {
    current = {
      time: bucket,
      open,
      high,
      low,
      close,
      volume,
      ema: emaPrev,
    }
    return
  }

  emaPrev = alpha * current.close + (1 - alpha) * emaPrev
  current.ema = emaPrev
  pushClosedCandle(current)

  current = {
    time: bucket,
    open,
    high,
    low,
    close,
    volume,
    ema: emaPrev,
  }
}

const ingestBar = (open: number, high: number, low: number, close: number, volume: number, timestamp: number) => {
  const bucket = Math.floor(timestamp / intervalMs) * intervalMs

  if (!current) {
    current = {
      time: bucket,
      open,
      high,
      low,
      close,
      volume,
      ema: emaPrev,
    }
    lastPrice = close
    lastDataAt = Date.now()
    tickCount += 1
    return
  }

  if (bucket < current.time) {
    return
  }

  if (bucket > current.time) {
    rolloverToNextBucket(bucket, open, high, low, close, volume)
  } else {
    current.high = Math.max(current.high, high)
    current.low = Math.min(current.low, low)
    current.close = close
    current.volume += volume
  }

  lastPrice = close
  lastDataAt = Date.now()
  tickCount += 1
}

const preloadHistory = async (apiKey: string, selectedSymbol: string, seedPrice: number, sessionId: number) => {
  candles = []
  current = null
  lastPrice = seedPrice
  emaPrev = seedPrice

  const crypto = parseCryptoPair(selectedSymbol)
  const intervalMinutes = mapResolutionToMinutes(intervalMs)
  const endpoint = crypto
    ? `https://www.alphavantage.co/query?function=CRYPTO_INTRADAY&symbol=${crypto.base}&market=${crypto.quote}&interval=${intervalMinutes}min&outputsize=full&apikey=${apiKey}`
    : `https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=${encodeURIComponent(selectedSymbol)}&interval=${intervalMinutes}min&outputsize=full&apikey=${apiKey}`

  const response = await fetch(endpoint)
  if (sessionId !== streamSession) {
    return
  }

  if (!response.ok) {
    throw new Error(`History preload failed (${response.status})`)
  }

  const payload = (await response.json()) as Record<string, unknown>
  if (sessionId !== streamSession) {
    return
  }

  const series = parseAlphaSeries(payload)
  if (!series) {
    throw new Error("History preload returned no data")
  }

  const sortedEntries = Object.entries(series)
    .map(([time, values]) => ({
      timestamp: Date.parse(time),
      open: Number(values["1. open"]),
      high: Number(values["2. high"]),
      low: Number(values["3. low"]),
      close: Number(values["4. close"]),
      volume: Number(values["5. volume"] || 0),
    }))
    .filter((bar) => Number.isFinite(bar.timestamp) && Number.isFinite(bar.open) && Number.isFinite(bar.high) && Number.isFinite(bar.low) && Number.isFinite(bar.close))
    .sort((left, right) => left.timestamp - right.timestamp)

  const relevant = sortedEntries.slice(Math.max(0, sortedEntries.length - historySize * 4))
  relevant.forEach((bar) => ingestBar(bar.open, bar.high, bar.low, bar.close, bar.volume, bar.timestamp))

  lastDataAt = Date.now()
  emit("connected")
}

const pollLivePrice = async (apiKey: string, selectedSymbol: string, sessionId: number) => {
  const crypto = parseCryptoPair(selectedSymbol)
  const endpoint = crypto
    ? `https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency=${crypto.base}&to_currency=${crypto.quote}&apikey=${apiKey}`
    : `https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=${encodeURIComponent(selectedSymbol)}&apikey=${apiKey}`

  try {
    const response = await fetch(endpoint)
    if (sessionId !== streamSession) {
      return
    }

    if (!response.ok) {
      throw new Error(`Live quote failed (${response.status})`)
    }

    const payload = (await response.json()) as Record<string, unknown>
    if (sessionId !== streamSession) {
      return
    }

    if (typeof payload.Note === "string") {
      throw new Error(payload.Note)
    }

    if (typeof payload["Error Message"] === "string") {
      throw new Error(payload["Error Message"] as string)
    }

    const price = crypto
      ? Number((payload["Realtime Currency Exchange Rate"] as Record<string, string> | undefined)?.["5. Exchange Rate"])
      : Number((payload["Global Quote"] as Record<string, string> | undefined)?.["05. price"])

    if (!Number.isFinite(price)) {
      return
    }

    ingestBar(lastPrice, Math.max(lastPrice, price), Math.min(lastPrice, price), price, 0, Date.now())
  } catch {
    if (sessionId === streamSession) {
      emit("error", "Alpha Vantage live quote failed")
    }
  }
}

const stop = () => {
  streamSession += 1

  if (emitTimer) {
    self.clearInterval(emitTimer)
  }
  emitTimer = undefined

  if (pollTimer) {
    self.clearInterval(pollTimer)
  }
  pollTimer = undefined
}

const start = async ({ symbol: selectedSymbol, intervalMs: nextIntervalMs, seedPrice, historySize: nextHistorySize, apiKey }: StreamConfig) => {
  stop()

  const sessionId = streamSession

  symbol = selectedSymbol
  intervalMs = nextIntervalMs
  historySize = nextHistorySize
  lastPrice = seedPrice
  emaPrev = seedPrice
  current = null
  candles = []
  tickCount = 0
  lastDataAt = Date.now()

  if (!apiKey) {
    emit("error", "Missing API key")
    return
  }

  try {
    await preloadHistory(apiKey, selectedSymbol, seedPrice, sessionId)
  } catch (error) {
    if (sessionId === streamSession) {
      const message = error instanceof Error ? error.message : "History preload failed"
      emit("error", message)
    }
    return
  }

  if (sessionId !== streamSession) {
    return
  }

  pollTimer = self.setInterval(() => {
    void pollLivePrice(apiKey, selectedSymbol, sessionId)
  }, pollIntervalMs)

  emitTimer = self.setInterval(() => {
    if (sessionId !== streamSession) {
      return
    }

    if (Date.now() - lastDataAt > noDataTimeoutMs) {
      emit("disconnected", "No market data received in 20 seconds")
      return
    }

    emit("connected")
  }, 1000)

  void pollLivePrice(apiKey, selectedSymbol, sessionId)
}

self.onmessage = (event: MessageEvent<WorkerMessage>) => {
  if (event.data.type === "stop") {
    stop()
    return
  }

  void start(event.data.payload)
}
