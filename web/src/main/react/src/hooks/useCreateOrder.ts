import { useMutation, useQueryClient } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { OrderData } from "api/types"
import { QueryClientKeys } from "global/constants"

export const useCreateOrder = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: OrderData) => getRestClient().ordersResource.createOrder(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Orders] })
    },
  })
}
