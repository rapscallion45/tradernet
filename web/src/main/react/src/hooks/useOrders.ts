import { useMutation, useQuery, useQueryClient, UseQueryResult } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { OrderData, OrderSummary } from "api/types"
import { QueryClientKeys } from "global/constants"

export const useOrders = (): UseQueryResult<OrderSummary[]> => {
  return useQuery({
    queryKey: [QueryClientKeys.Orders],
    queryFn: () => getRestClient().ordersResource.getOrders(),
  })
}

export const useCreateOrder = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: OrderData) => getRestClient().ordersResource.createOrder(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Orders] })
    },
  })
}
