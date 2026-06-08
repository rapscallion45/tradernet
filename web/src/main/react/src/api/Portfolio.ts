import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { Get, PortfolioSummary } from "api/types"

export class PortfolioResource extends RestResource<PortfolioSummary, never> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "portfolio")
  }

  getPortfolio(currency?: string): Get<PortfolioSummary> {
    return this._get(currency ? { queryParams: { currency } } : undefined)
  }
}
