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

type FinnhubCandlesResponse = {
  c: number[]
  h: number[]
  l: number[]
  o: number[]
  t: number[]
  v: number[]
  s: "ok" | "no_data"
}

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
let streamSession = 0
let lastDataAt = 0

const emaPeriod = 14
const alpha = 2 / (emaPeriod + 1)
const noDataTimeoutMs = 20_000

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

const mapSymbolForFinnhub = (selectedSymbol: string): string => {
  if (selectedSymbol === "BTCUSD") {
    return "BINANCE:BTCUSDT"
  }
  if (selectedSymbol === "ETHUSD") {
    return "BINANCE:ETHUSDT"
  }
  return selectedSymbol
}

const mapResolution = (value: number): string => {
  if (value <= 60_000) {
    return "1"
  }
  if (value <= 5 * 60_000) {
    return "5"
  }
  if (value <= 15 * 60_000) {
    return "15"
  }
  if (value <= 30 * 60_000) {
    return "30"
  }
  if (value <= 60 * 60_000) {
    return "60"
  }
  if (value <= 24 * 60 * 60_000) {
    return "D"
  }
  if (value <= 7 * 24 * 60 * 60_000) {
    return "W"
  }
  return "M"
}

const buildHistoryUrl = (finnhubSymbol: string, token: string) => {
  const now = Math.floor(Date.now() / 1000)
  const clampedLookbackMs = Math.min(historySize * Math.max(intervalMs, 60_000), 5 * 365 * 24 * 60 * 60_000)
  const from = Math.max(0, now - Math.floor(clampedLookbackMs / 1000))
  const resolution = mapResolution(intervalMs)
  const endpoint = finnhubSymbol.includes(":") ? "crypto/candle" : "stock/candle"
  return `https://finnhub.io/api/v1/${endpoint}?symbol=${encodeURIComponent(finnhubSymbol)}&resolution=${resolution}&from=${from}&to=${now}&token=${token}`
}

const hydrateFromHistory = (response: FinnhubCandlesResponse, seedPrice: number) => {
  candles = []
  current = null
  lastPrice = seedPrice
  emaPrev = seedPrice

  if (response.s !== "ok" || response.t.length === 0) {
    return
  }

  const rawCandles: Candle[] = response.t.map((timestamp, index) => {
    const close = response.c[index]
    return {
      time: timestamp * 1000,
      open: response.o[index],
      high: response.h[index],
      low: response.l[index],
      close,
      volume: response.v[index] ?? 0,
      ema: close,
    }
  })

  rawCandles.sort((left, right) => left.time - right.time)

  rawCandles.forEach((bar, index) => {
    emaPrev = alpha * bar.close + (1 - alpha) * emaPrev
    bar.ema = emaPrev

    if (index === rawCandles.length - 1) {
      current = bar
      lastPrice = bar.close
      return
    }

    candles.push(bar)
  })

  if (candles.length > historySize) {
    candles = candles.slice(candles.length - historySize)
  }

  lastDataAt = Date.now()
}

const preloadHistory = async (apiKey: string, selectedSymbol: string, seedPrice: number, sessionId: number) => {
  const finnhubSymbol = mapSymbolForFinnhub(selectedSymbol)
  const url = buildHistoryUrl(finnhubSymbol, apiKey)

  try {
    const response = await fetch(url)
    if (sessionId !== streamSession) {
      return
    }

    if (!response.ok) {
      emit("error", `History preload failed (${response.status})`)
      return
    }

    const payload = (await response.json()) as FinnhubCandlesResponse
    if (sessionId !== streamSession) {
      return
    }

    hydrateFromHistory(payload, seedPrice)
    emit("connected")
  } catch {
    if (sessionId === streamSession) {
      emit("error", "History preload failed")
    }
  }
}

const pushClosedCandle = (candle: Candle) => {
  candles.push(candle)
  if (candles.length > historySize) {
    candles = candles.slice(candles.length - historySize)
  }
}

const rolloverToNextBucket = (bucket: number, price: number, volume: number) => {
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
    return
  }

  emaPrev = alpha * current.close + (1 - alpha) * emaPrev
  current.ema = emaPrev
  pushClosedCandle(current)

  current = {
    time: bucket,
    open: lastPrice,
    high: price,
    low: price,
    close: price,
    volume,
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
    lastDataAt = Date.now()
    tickCount += 1
    return
  }

  if (bucket < current.time) {
    return
  }

  if (bucket > current.time) {
    rolloverToNextBucket(bucket, price, volume)
  } else {
    current.high = Math.max(current.high, price)
    current.low = Math.min(current.low, price)
    current.close = price
    current.volume += volume
  }

  lastPrice = price
  lastDataAt = Date.now()
  tickCount += 1
}

const stop = () => {
  streamSession += 1

  if (emitTimer) {
    self.clearInterval(emitTimer)
  }
  emitTimer = undefined

  if (socket) {
    socket.close()
    socket = null
  }
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
    emit("error", "Missing VITE_FINNHUB_API_KEY")
    return
  }

  await preloadHistory(apiKey, selectedSymbol, seedPrice, sessionId)
  if (sessionId !== streamSession) {
    return
  }

  const finnhubSymbol = mapSymbolForFinnhub(selectedSymbol)
  socket = new WebSocket(`wss://ws.finnhub.io?token=${apiKey}`)

  socket.onopen = () => {
    if (sessionId !== streamSession) {
      return
    }

    socket?.send(JSON.stringify({ type: "subscribe", symbol: finnhubSymbol }))
    emit("connected")
  }

  socket.onmessage = (event) => {
    if (sessionId !== streamSession) {
      return
    }

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
    if (sessionId !== streamSession) {
      return
    }
    emit("error", "Finnhub socket error")
  }

  socket.onclose = () => {
    if (sessionId !== streamSession) {
      return
    }
    emit("disconnected")
  }

  emitTimer = self.setInterval(() => {
    if (sessionId !== streamSession) {
      return
    }

    const isOpen = socket?.readyState === WebSocket.OPEN
    if (isOpen && Date.now() - lastDataAt > noDataTimeoutMs) {
      emit("disconnected", "No market data received in 20 seconds")
      return
    }

    emit(isOpen ? "connected" : "disconnected")
  }, 1000)
}

self.onmessage = (event: MessageEvent<WorkerMessage>) => {
  if (event.data.type === "stop") {
    stop()
    return
  }

  void start(event.data.payload)
}
