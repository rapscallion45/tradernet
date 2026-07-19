import { UseMutationResult } from "@tanstack/react-query"
import { isAxiosError } from "axios"
import { useToast } from "hooks/useToast"
import { ApiErrorBody } from "api/types"

export type ApiErrorModelWithStatus = {
  status: number
  error: NonNullable<ApiErrorBody["error"]>
}

export type PartialHandlers<ReturnType, PayloadType = ReturnType> = Partial<CrudHandlers<ReturnType, PayloadType>>

/**
 * Convenience type to avoid having to pass the error type every time, when it's mostly unknown.
 *
 * Also makes it clear what types UseMutationResult is expecting (return type is TData, and payload type is TVariables)
 */
export type MutationResult<ReturnType, PayloadType = ReturnType> = UseMutationResult<ReturnType, unknown, PayloadType>

interface CrudHandlers<ReturnType, PayloadType = ReturnType> {
  success: (data: ReturnType, payload?: PayloadType) => void
  apiModel: (data: ApiErrorModelWithStatus) => void
  notFound: (payload?: PayloadType) => void
  unauthorised: () => void
  other: (err: unknown, statusCode?: number) => void
}

export default function useCrudHandling<ReturnType, PayloadType = ReturnType>(): [
  (error: unknown, handlers: PartialHandlers<ReturnType, PayloadType>, payload?: PayloadType) => void,
  (data: ReturnType, handlers: PartialHandlers<ReturnType, PayloadType>, payload?: PayloadType) => void,
] {
  const { toast } = useToast()

  function handleSuccess(data: ReturnType, handlers: PartialHandlers<ReturnType, PayloadType>, payload?: PayloadType) {
    if (handlers.success) {
      handlers.success(data, payload)
    } else {
      console.log("Success scenario unhandled (data, handlers)", data, handlers)
    }
  }

  function handleError(error: unknown, handlers: PartialHandlers<ReturnType, PayloadType>, payload?: PayloadType) {
    if (isAxiosError(error) && error.response?.status === 401) {
      toast({
        id: "SessionExpired",
        title: "Session Expired",
        message: "Your session has expired, please login again",
        variant: "error",
        timestamp: Date.now(),
      })
    }

    if (isAxiosError(error) && error.response?.data && isApiErrorBody(error.response?.data) && error.response?.status && handlers.apiModel) {
      handlers.apiModel({
        status: error.response?.status,
        error: error.response?.data.error,
      })
    } else if (isAxiosError(error) && error.response?.status === 404 && handlers.notFound) {
      handlers.notFound(payload)
    } else if (isAxiosError(error) && error.response?.status && handlers.other) {
      handlers.other(error, error.response?.status)
    } else if (handlers.other) {
      handlers.other(error)
    } else {
      console.error("Error scenario unhandled (error, handlers, payload)", error, handlers, payload)
    }
  }

  return [handleError, handleSuccess]
}

export function isApiErrorBody(data: unknown): data is ApiErrorBody & { error: NonNullable<ApiErrorBody["error"]> } {
  return (data as ApiErrorBody).error?.errorMessage !== undefined
}
