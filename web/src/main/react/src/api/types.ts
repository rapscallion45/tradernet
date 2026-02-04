/**
 * Generic Models used throughout the API
 */
import { AxiosRequestConfig } from "axios"

export type ApiErrorModel = {
  errorCode: number
  errorMessage: string
  hostName: string
  timestamp: number
}

export type ApiErrorBody = {
  error?: {
    errorMessage?: string
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
  UserNotFound = "USER_NOT_FOUND",
  InvalidRequest = "INVALID_REQUEST",
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

/** Reset password validation settings */
export type PasswordSettings = {
  alphasAndNumericsEnabled: boolean
  minLength: number
  maxLength: number
  repetitionThreshold: number
  startsWithAlphaEnabled: boolean
  upperAndLowerAlphasEnabled: boolean
}

/**
 * Users
 */

/** User */
export interface User {
  id?: number
  username: string
}

/**
 * Orders
 */

/** Order */
export type OrderData = {
  symbol: string
  quantity: number
}
