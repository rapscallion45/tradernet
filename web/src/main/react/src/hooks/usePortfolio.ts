import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { PortfolioSummary } from "api/types"
import { QueryClientKeys } from "global/constants"
import { useActiveCurrency } from "hooks/useActiveCurrency"

/**
 * Returns authenticated user's portfolio summary.
 */
export const usePortfolio = (): UseSuspenseQueryResult<PortfolioSummary> => {
  const currency = useActiveCurrency()

  return useSuspenseQuery({
    queryKey: [QueryClientKeys.Portfolio, currency],
    queryFn: () => getRestClient().portfolioResource.getPortfolio(currency),
  })
}
