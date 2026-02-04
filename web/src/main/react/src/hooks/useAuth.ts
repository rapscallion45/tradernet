import { useMutation } from "@tanstack/react-query"
import type { AxiosError } from "axios"
import { useToast } from "hooks/useToast"
import { ApiErrorBody, SafeResult, LoginData, LoginResponse, LogoutResponse } from "api/types"
import useCrudHandling, { PartialHandlers } from "hooks/useCrudHandling"
import { getErrorMessage } from "api/util"
import { getRestClient } from "../api/RestClient"

/**
 * Custom hook pertaining logic for User login
 */
export function useLogin() {
  const [handleError] = useCrudHandling<LoginResponse>()
  const { toast } = useToast()
  const toastId = "login-request"

  const defaultHandlers = (variables: LoginData): PartialHandlers<LoginResponse> => ({
    apiModel: (error) =>
      toast({
        id: toastId,
        variant: "error",
        title: `Failed to login as "${variables.username}".`,
        message: getErrorMessage(error),
        timestamp: Date.now(),
      }),
    notFound: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Login failed",
        message: "Resource was not found.",
        timestamp: Date.now(),
      }),
    unauthorised: () => window.location.reload(),
    other: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Login failed",
        message: "An unexpected error occurred while trying to login.",
        timestamp: Date.now(),
      }),
  })

  const mutation = useMutation<LoginResponse, AxiosError<ApiErrorBody>, LoginData>({
    mutationFn: (data) => getRestClient().authResource.login(data),
    onError: (error, variables) => {
      try {
        handleError(error, { ...defaultHandlers(variables) })
      } catch {
        toast({
          id: toastId,
          variant: "error",
          title: "Login failed",
          message: "An unexpected error occurred while handling the login error.",
          timestamp: Date.now(),
        })
      }
    },
  })

  /** helper for encapsulating the try catch block with the safe result type */
  const execute = async (vars: LoginData): Promise<SafeResult<LoginResponse>> => {
    try {
      const data = await mutation.mutateAsync(vars)
      return { ok: true, data }
    } catch (error) {
      return { ok: false, error }
    }
  }

  return { ...mutation, execute }
}

/**
 * Custom hook pertaining logic for User logout
 */
export function useLogout() {
  const [handleError] = useCrudHandling<LogoutResponse>()
  const { toast } = useToast()
  const toastId = "logout-request"

  const defaultHandlers = (): PartialHandlers<LogoutResponse> => ({
    apiModel: (error) =>
      toast({
        id: toastId,
        variant: "error",
        title: "Logout failed",
        message: getErrorMessage(error),
        timestamp: Date.now(),
      }),
    notFound: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Logout failed",
        message: "Resource was not found.",
        timestamp: Date.now(),
      }),
    unauthorised: () => window.location.reload(),
    other: () =>
      toast({
        id: toastId,
        variant: "error",
        title: "Logout failed",
        message: "An unexpected error occurred while trying to logout.",
        timestamp: Date.now(),
      }),
  })

  const mutation = useMutation<LogoutResponse, AxiosError<ApiErrorBody>, void>({
    mutationFn: () => getRestClient().authResource.logout(),
    onError: (error) => {
      try {
        handleError(error, { ...defaultHandlers() })
      } catch {
        toast({
          id: toastId,
          variant: "error",
          title: "Logout failed",
          message: "An unexpected error occurred while handling the logout error.",
          timestamp: Date.now(),
        })
      }
    },
  })

  /** helper for encapsulating the try catch block with the safe result type */
  const execute = async (): Promise<SafeResult<LogoutResponse>> => {
    try {
      const data = await mutation.mutateAsync()
      return { ok: true, data }
    } catch (error) {
      return { ok: false, error }
    }
  }

  return { ...mutation, execute }
}
