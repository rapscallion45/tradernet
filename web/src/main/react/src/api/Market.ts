import { ApiInterface } from "api/ApiInterface"
import { List, MarketBar } from "api/types"
import { RestResource } from "api/RestResource"

/**
 * Market resource definition
 */
export class MarketResource extends RestResource<MarketBar> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "market")
  }

  getBars(symbol: string, interval = "1S", limit = 1): List<MarketBar> {
    return this.typedSubPath<MarketBar>("bars")._list({
      queryParams: {
        symbol,
        interval,
        limit,
      },
    })
  }
}
