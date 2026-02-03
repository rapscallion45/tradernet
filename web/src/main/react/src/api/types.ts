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
  user: {
    id: number
    username: string
  }
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
