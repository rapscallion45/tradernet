/**
 * Generic Models used throughout the API
 */
import { AxiosRequestConfig } from "axios"

export type ApiErrorBody = {
  error?: {
    status?: number
    code?: string
    errorMessage?: string
    timestamp?: number
    referenceId?: string
  }
}

export type SafeResult<T> = { ok: true; data: T } | { ok: false; error: unknown }

export type Get<T> = Promise<T>
export type Delete<T> = Promise<T>
export type Post<T> = Promise<T>
export type Put<T> = Promise<T>
export type List<T> = Promise<T[]>
export type Void = Promise<void>

type BespokeConfig = {
  axios?: AxiosRequestConfig
}

export type BaseApiRequest = {
  pathParams?: Array<string | number>
  queryParams?: Record<string, string | number | boolean | string[]>
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  headers?: Record<string, any>
  validateStatus?: (status: number, response: ApiResponse<unknown>) => boolean
  bespokeConfig?: BespokeConfig
}

export type ApiResponse<DataType> = {
  data: DataType
  headers: unknown
  status: number
  statusText: string
}

export type ApiRequest<PayloadType> = BaseApiRequest & {
  data?: PayloadType
}

export type ApiRequestWithBaseUrl<T> = ApiRequest<T> & {
  resourcePath: string
}

export type GlobalErrorCallback = (err: unknown, config: ApiRequestWithBaseUrl<unknown>) => void

export type GenericPropertiesObject = Record<string, string>

export type ContentDispositionType = "inline" | "attachment"

type PagingQueryParameters = {
  page: string
  perPage: string
}

export type ClientAuthConfiguration = {
  useCookies: boolean
  useToken: boolean
  tokenConfig: {
    asBearerToken: boolean
    headerName: string
  }
}

/**
 * Health
 */

export type HealthResponse = {
  status: string
}

export type MarketBar = {
  symbol: string
  bucketStart: number
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export type MarketContextSnapshot = {
  etfFlowZScore: number
  exchangeOutflowZScore: number
  fundingRateZScore: number
  openInterestChangeZScore: number
  mvrvZScore: number
  liquidityGrowthZScore: number
  sentimentZScore: number
  etfFlowBullishPercent: number
  exchangeOutflowBullishPercent: number
  fundingRateBullishPercent: number
  openInterestChangeBullishPercent: number
  mvrvBullishPercent: number
  liquidityGrowthBullishPercent: number
  sentimentBullishPercent: number
  anyMarketScoreInputAvailable: boolean
  etfFlowAvailable: boolean
  exchangeOutflowAvailable: boolean
  fundingRateAvailable: boolean
  openInterestChangeAvailable: boolean
  mvrvAvailable: boolean
  liquidityGrowthAvailable: boolean
  sentimentAvailable: boolean
  available?: boolean
}

export type ExplanationItem = {
  key: string
  label?: string
  value?: string
  numericValue?: number
}

export type MarketForecast = {
  symbol: string
  horizonDays: number
  probabilityPositiveReturn: number
  expectedReturn: number
  bullScore: number
  model: string
  drivers: ExplanationItem[]
  narrative?: string
}

export type OrderBookStatus = "LIVE" | "SYNCING" | "SNAPSHOT_ONLY" | "STALE" | "UNAVAILABLE"

export type OrderBookLevel = {
  price: number
  quantity: number
  notional: number
  cumulativeQuantity: number
  cumulativeNotional: number
  depthPercent: number
}

export type OrderBookSnapshot = {
  symbol: string
  quoteCurrency: string
  status: OrderBookStatus
  source: string
  aggregation: string
  message: string
  eventTime: number
  lastUpdateId: number
  updateLatencyMs: number
  resyncCount: number
  exchangeSnapshotLimit: number
  requestedLevels: number
  stale: boolean
  bestBid: number
  bestAsk: number
  midPrice: number
  spread: number
  spreadPercent: number
  bidDepthNotional: number
  askDepthNotional: number
  depthImbalancePercent: number
  bids: OrderBookLevel[]
  asks: OrderBookLevel[]
}

/**
 * Auth
 */

/** Login Request payload */
export type LoginData = {
  username: string
  password: string
}

/** Login Response payload */
export type LoginResponse = {
  status: LoginStatus
}

/** Login response status types */
export enum LoginStatus {
  Success = "SUCCESS",
  IncorrectCredentials = "INCORRECT_CREDENTIALS",
  InvalidRequest = "INVALID_REQUEST",
  RateLimited = "RATE_LIMITED",
  AccountPasswordExpired = "ACCOUNT_PASSWORD_EXPIRED",
  Unknown = "UNKNOWN",
}

/** Logout response */
export type LogoutResponse = {
  message: string
}

/** Auth session info response */
export type SessionInfo = {
  id: number
  username: string
}

export type ForgotPasswordData = {
  newPassword: string
}

export type MessageResponse = {
  message: string
}

/**
 * Users
 */

/** User */
export interface User {
  id?: number
  username: string
  fullName?: string
  emailAddress?: string
  accountExpiry?: string
  lastLogin?: string
  changePasswordNextLogin?: boolean
  roleNames?: string[]
}

/**
 * Orders
 */

/** Order */
export type OrderSide = "BUY" | "SELL"

export type OrderData = {
  symbol: string
  side: OrderSide
  quantity: number
  price: number
}

export type OrderSummary = {
  id: number
  userId: number
  symbol: string
  side: OrderSide
  currency: string
  quantity: number
  price: number
  status: string
  createdAt: string
  closedAt?: string
  closePrice?: number
  currentPrice?: number
  pnl?: number
  pnlPercent?: number
  netValue?: number
  timing?: "GOOD" | "BAD" | "NEUTRAL" | "CLOSED"
  aiPrediction?: "BUY" | "SELL" | "HOLD" | string
  bullScore?: number
}

/** Portfolio */

export type PortfolioAsset = {
  symbol: string
  quantity: number
  averageCost: number
  currentPrice: number
  totalCost: number
  marketValue: number
  profitLoss: number
  profitLossPercent: number
}

export type PortfolioHistoryPoint = {
  timestamp: number
  accountValue: number
  events?: PortfolioHistoryEvent[]
}

export type PortfolioHistoryEvent = {
  symbol: string
  side: OrderSide | string
  quantity: number
  price: number
  timestamp: number
}

export type PortfolioSummary = {
  currency: string
  totalMarketValue: number
  totalCost: number
  totalProfitLoss: number
  totalProfitLossPercent: number
  assets: PortfolioAsset[]
  history: PortfolioHistoryPoint[]
}
