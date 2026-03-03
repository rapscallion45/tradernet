/**
 * Global constant definitions
 */

/** Query client keys definitions */
export enum QueryClientKeys {
  /** User session */
  Session = "Session",

  /** Server health check */
  HealthCheck = "HealthCheck",

  /** Users */
  Users = "Users",

  /** Roles */
  Roles = "Roles",

  /** Resource Entities */
  ResourceEntities = "ResourceEntities",

  /** Groups */
  Groups = "Groups",

  /** Orders */
  Orders = "Orders",

  /** Market Bars */
  MarketBars = "MarketBars",
}

/** Available chart/order symbols */
export const CHART_SYMBOL_OPTIONS = ["BTCUSDT"] as const

/** Default chart/order symbol */
export const DEFAULT_CHART_SYMBOL = CHART_SYMBOL_OPTIONS[0]
