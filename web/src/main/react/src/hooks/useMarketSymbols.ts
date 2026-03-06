import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"

export const useMarketSymbols = (currency: string) => {
  return useQuery({
    queryKey: [QueryClientKeys.MarketSymbols, currency],
    queryFn: async () => {
      const symbols = await getRestClient().marketResource.getSymbols(currency)
      return symbols.length > 0 ? symbols : [DEFAULT_CHART_SYMBOL]
    },
    staleTime: 15 * 60 * 1000,
  })
}
