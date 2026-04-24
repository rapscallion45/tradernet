import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { PortfolioSummary } from "api/types"
import { QueryClientKeys } from "global/constants"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"

/**
 * Returns authenticated user's portfolio summary.
 */
export const usePortfolio = (): UseSuspenseQueryResult<PortfolioSummary> => {
  const { currency } = useCurrencyPreference()

  return useSuspenseQuery({
    queryKey: [QueryClientKeys.Portfolio, currency],
    queryFn: () => getRestClient().portfolioResource.getPortfolio(currency),
  })
}
