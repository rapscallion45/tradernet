/**
 * Generic Models used throughout the API
 */

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

export enum LoginStatus {
  Success = "SUCCESS",
  IncorrectCredentials = "INCORRECT_CREDENTIALS",
  UserNotFound = "USER_NOT_FOUND",
  InvalidRequest = "INVALID_REQUEST",
  AccountPasswordExpired = "ACCOUNT_PASSWORD_EXPIRED",
  Unknown = "UNKNOWN",
}

/** Auth session response */
export type AuthSessionResponse = {
  id: number
  username: string
}

/** Logout response */
export type LogoutResponse = {
  message: string
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
