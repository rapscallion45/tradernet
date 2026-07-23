import type { AxiosError } from "axios"
import { ApiErrorBody } from "api/types"

export const getErrorMessage = (err: unknown) => {
  const ax = err as AxiosError<ApiErrorBody>
  const apiError = err as ApiErrorBody
  const wrappedApiError = err as { error?: ApiErrorBody["error"] | string; message?: string }
  const wrappedErrorMessage = typeof wrappedApiError?.error === "string" ? wrappedApiError.error : wrappedApiError?.error?.errorMessage
  return ax?.response?.data?.error?.errorMessage ?? apiError?.error?.errorMessage ?? wrappedErrorMessage ?? wrappedApiError?.message ?? ax?.message ?? "An unexpected error occurred."
}

export function stripStartAndEndSlash(str: string): string {
  const stripStart = str.startsWith("/")
  const stripEnd = str.endsWith("/")
  return str.slice(stripStart ? 1 : 0, stripEnd ? -1 : undefined)
}

export function buildUrl(...parts: string[]): string {
  return parts.map(stripStartAndEndSlash).join("/")
}
