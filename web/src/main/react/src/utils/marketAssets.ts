const QUOTE_SUFFIXES = ["USDT", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "BRL", "TRY", "BTC", "ETH", "BNB"]

export const getBaseAsset = (symbol: string): string => {
  const upper = symbol.toUpperCase()
  const matchedQuote = QUOTE_SUFFIXES.find((quote) => upper.endsWith(quote))
  return matchedQuote ? upper.slice(0, upper.length - matchedQuote.length) : upper
}

export const getAssetLogoUrl = (symbol: string): string => {
  const baseAsset = getBaseAsset(symbol).toLowerCase()
  return `https://cdn.jsdelivr.net/gh/spothq/cryptocurrency-icons@master/128/color/${baseAsset}.png`
}
