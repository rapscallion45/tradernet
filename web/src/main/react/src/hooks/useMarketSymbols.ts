import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"

const SYMBOL_QUOTE_CURRENCY = "USD"

export const useMarketSymbols = () => {
  return useQuery({
    queryKey: [QueryClientKeys.MarketSymbols, SYMBOL_QUOTE_CURRENCY],
    queryFn: async () => {
      const symbols = await getRestClient().marketResource.getSymbols(SYMBOL_QUOTE_CURRENCY)
      return symbols.length > 0 ? symbols : [DEFAULT_CHART_SYMBOL]
    },
    staleTime: 15 * 60 * 1000,
  })
}
