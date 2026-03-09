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

  /** Market symbols */
  MarketSymbols = "MarketSymbols",

  /** Market currencies */
  MarketCurrencies = "MarketCurrencies",
}

/** Default chart/order symbol */
export const DEFAULT_CHART_SYMBOL = "BTCUSDT"
