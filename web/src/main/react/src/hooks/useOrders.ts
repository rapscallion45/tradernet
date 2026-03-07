import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { OrderSummary } from "api/types"
import { QueryClientKeys } from "global/constants"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"

/**
 * Hook for retrieving order history in suspense mode.
 */
export const useOrders = (): UseSuspenseQueryResult<OrderSummary[]> => {
  const { currency } = useCurrencyPreference()

  return useSuspenseQuery({
    queryKey: [QueryClientKeys.Orders, currency],
    queryFn: () => getRestClient().ordersResource.getOrders(undefined, currency),
  })
}
