import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"

export const useMarketSymbols = () => {
  return useQuery({
    queryKey: [QueryClientKeys.MarketSymbols],
    queryFn: async () => {
      const symbols = await getRestClient().marketResource.getSymbols()
      return symbols.length > 0 ? symbols : [DEFAULT_CHART_SYMBOL]
    },
    staleTime: 15 * 60 * 1000,
  })
}
