import { useMutation, useQueryClient } from "@tanstack/react-query"
import type { AxiosError } from "axios"
import { getRestClient } from "api/RestClient"
import { ApiErrorBody, OrderData, OrderSummary } from "api/types"
import { getErrorMessage } from "api/util"
import { QueryClientKeys } from "global/constants"
import useCrudHandling, { PartialHandlers } from "hooks/useCrudHandling"
import { useToast } from "hooks/useToast"

/**
 * Hook for creating a new order and refreshing the orders cache.
 */
export const useCreateOrder = () => {
  const queryClient = useQueryClient()
  const [handleError, handleSuccess] = useCrudHandling<OrderSummary, OrderData>()
  const { toast } = useToast()
  const toastId = "create-order"

  const defaultHandlers = (payload: OrderData): PartialHandlers<OrderSummary, OrderData> => ({
    success: () =>
      toast({
        id: toastId,
        variant: "success",
        title: "Order submitted",
        message: `Submitted ${payload.side} ${payload.quantity} ${payload.symbol.toUpperCase()} @ ${payload.price}`,
        timestamp: Date.now(),
      }),
    apiModel: (error) =>
      toast({
        id: toastId,
        variant: "error",
        title: "Failed to submit order",
        message: getErrorMessage(error),
        timestamp: Date.now(),
      }),
    notFound: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Failed to submit order",
        message: "Order endpoint was not found.",
        timestamp: Date.now(),
      }),
    unauthorised: () => window.location.reload(),
    other: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Failed to submit order",
        message: "Please check your input and try again.",
        timestamp: Date.now(),
      }),
  })

  return useMutation<OrderSummary, AxiosError<ApiErrorBody>, OrderData>({
    mutationFn: (payload) => getRestClient().ordersResource.createOrder(payload),
    onMutate: () => {
      toast({
        id: toastId,
        variant: "info",
        title: "Submitting order...",
        message: "Order submission in progress.",
        timestamp: Date.now(),
        loading: true,
      })
    },
    onSuccess: (data, payload) => {
      handleSuccess(data, { ...defaultHandlers(payload) }, payload)
      void queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Orders] })
    },
    onError: (error, payload) => {
      handleError(error, { ...defaultHandlers(payload) }, payload)
    },
  })
}
