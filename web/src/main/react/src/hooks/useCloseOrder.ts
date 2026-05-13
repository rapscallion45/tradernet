import { useMutation, useQueryClient } from "@tanstack/react-query"
import type { AxiosError } from "axios"
import { getRestClient } from "api/RestClient"
import { ApiErrorBody, OrderSummary } from "api/types"
import { getErrorMessage } from "api/util"
import { QueryClientKeys } from "global/constants"
import useCrudHandling, { PartialHandlers } from "hooks/useCrudHandling"
import { useToast } from "hooks/useToast"

/**
 * Hook for closing an open order and refreshing the orders cache.
 */
export const useCloseOrder = () => {
  const queryClient = useQueryClient()
  const [handleError, handleSuccess] = useCrudHandling<OrderSummary, number>()
  const { toast } = useToast()
  const toastId = "close-order"

  const defaultHandlers = (orderId: number): PartialHandlers<OrderSummary, number> => ({
    success: () =>
      toast({
        id: toastId,
        variant: "success",
        title: "Order closed",
        message: `Closed order #${orderId}.`,
        timestamp: Date.now(),
      }),
    apiModel: (error) =>
      toast({
        id: toastId,
        variant: "error",
        title: "Failed to close order",
        message: getErrorMessage(error),
        timestamp: Date.now(),
      }),
    notFound: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Order not found",
        message: `Order #${orderId} could not be found.`,
        timestamp: Date.now(),
      }),
    unauthorised: () => window.location.reload(),
    other: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Failed to close order",
        message: "Please try again.",
        timestamp: Date.now(),
      }),
  })

  return useMutation<OrderSummary, AxiosError<ApiErrorBody>, number>({
    mutationFn: (orderId) => getRestClient().ordersResource.closeOrder(orderId),
    onSuccess: (data, orderId) => {
      handleSuccess(data, { ...defaultHandlers(orderId) }, orderId)
      void queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Orders] })
      void queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Portfolio] })
    },
    onError: (error, orderId) => {
      handleError(error, { ...defaultHandlers(orderId) }, orderId)
    },
  })
}
