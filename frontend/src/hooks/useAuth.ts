import { useMutation } from "@tanstack/react-query"
import apiClient from "api/apiClient"
import { LoginData, LoginResponse, LogoutResponse } from "api/types"

/**
 * Custom hook pertaining logic for User login
 */
export function useLogin() {
  return useMutation<LoginResponse, unknown, LoginData>({
    mutationFn: (data: LoginData) => apiClient.post<LoginResponse>("/auth/login", data).then((res) => res.data),
  })
}

/**
 * Custom hook pertaining logic for User logout
 */
export function useLogout() {
  return useMutation<LogoutResponse, void, void>({ mutationFn: () => apiClient.post<LogoutResponse>("/auth/logout").then((res) => res.data) })
}
