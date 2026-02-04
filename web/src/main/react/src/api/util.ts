import type { AxiosError } from "axios"
import { ApiErrorBody } from "api/types"

export const getErrorMessage = (err: unknown) => {
  const ax = err as AxiosError<ApiErrorBody>
  return ax?.response?.data?.error?.errorMessage ?? ax?.message ?? "An unexpected error occurred."
}

export function stripStartAndEndSlash(str: string): string {
  const stripStart = str.startsWith("/")
  const stripEnd = str.endsWith("/")
  return str.slice(stripStart ? 1 : 0, stripEnd ? -1 : undefined)
}

export function buildUrl(...parts: string[]): string {
  return parts.map(stripStartAndEndSlash).join("/")
}
