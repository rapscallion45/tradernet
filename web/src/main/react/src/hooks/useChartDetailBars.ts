import { useQuery } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"

const refetchIntervalMs = 15_000

/**
 * Fetches chart detail bars for the currently selected symbol.
 */
export const useChartDetailBars = (selectedSymbol: string, currency: string) => {
  return useQuery({
    queryKey: [QueryClientKeys.MarketBars, selectedSymbol, currency, "charts-detail"],
    queryFn: () => getRestClient().marketResource.getBars(selectedSymbol, "1M", 24, currency),
    refetchInterval: refetchIntervalMs,
  })
}
