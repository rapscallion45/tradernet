import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { OrderSummary } from "api/types"
import { QueryClientKeys } from "global/constants"

/**
 * Hook for retrieving order history in suspense mode.
 */
export const useOrders = (): UseSuspenseQueryResult<OrderSummary[]> => {
  return useSuspenseQuery({
    queryKey: [QueryClientKeys.Orders],
    queryFn: () => getRestClient().ordersResource.getOrders(),
  })
}
