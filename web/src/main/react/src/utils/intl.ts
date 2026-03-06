const DEFAULT_LOCALE = "en-US"
const DEFAULT_CURRENCY = "USD"

const REGION_CURRENCY_MAP: Record<string, string> = {
  US: "USD",
  GB: "GBP",
  JP: "JPY",
  CA: "CAD",
  AU: "AUD",
  NZ: "NZD",
  CH: "CHF",
  SE: "SEK",
  NO: "NOK",
  DK: "DKK",
  BR: "BRL",
  MX: "MXN",
  IN: "INR",
  CN: "CNY",
  KR: "KRW",
  SG: "SGD",
  HK: "HKD",
  ZA: "ZAR",
  AE: "AED",
}

const isSupportedCurrency = (currency: string, locale = DEFAULT_LOCALE): boolean => {
  try {
    new Intl.NumberFormat(locale, {
      style: "currency",
      currency,
    }).format(0)
    return true
  } catch {
    return false
  }
}

const getNavigatorLocale = (): string | undefined => {
  if (typeof navigator === "undefined") {
    return undefined
  }

  return navigator.languages?.[0] || navigator.language
}

export const getUserLocale = (): string => getNavigatorLocale() || DEFAULT_LOCALE

export const inferCurrencyFromLocale = (locale = getUserLocale()): string => {
  const region = locale.match(/-([A-Za-z]{2})\b/)?.[1]?.toUpperCase()
  if (!region) {
    return DEFAULT_CURRENCY
  }

  return REGION_CURRENCY_MAP[region] || DEFAULT_CURRENCY
}

export const getUserCurrency = (): string => DEFAULT_CURRENCY

export const setUserCurrency = (_currency: string) => undefined

export const formatDateTime = (value?: string | number | Date, locale = getUserLocale()): string => {
  if (value === undefined || value === null || value === "") {
    return ""
  }

  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "medium",
  }).format(date)
}

export const formatNumber = (value: number, options?: Intl.NumberFormatOptions, locale = getUserLocale()): string => {
  return new Intl.NumberFormat(locale, options).format(value)
}

export const formatCurrency = (value: number, currency = getUserCurrency(), locale = getUserLocale()): string => {
  const resolvedCurrency = isSupportedCurrency(currency, locale) ? currency : DEFAULT_CURRENCY

  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: resolvedCurrency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 6,
  }).format(value)
}
