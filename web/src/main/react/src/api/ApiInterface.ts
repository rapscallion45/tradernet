import { ApiRequestWithBaseUrl, ApiResponse, Get, GlobalErrorCallback } from "api/types"

export enum HandlerOrder {
  PRE_AUTH = "PRE_AUTH",
  POST_AUTH = "POST_AUTH",
}

export type InterceptorHandler = {
  handler: () => Promise<void>
  order: HandlerOrder
}

export enum ApiRequestType {
  Get = "get",
  Create = "create",
  Update = "update",
  Delete = "delete",
}

export interface ApiInterface {
  onError?: GlobalErrorCallback

  fetch<ReturnType, PayloadType = ReturnType>(type: ApiRequestType, config: ApiRequestWithBaseUrl<PayloadType>): Get<ApiResponse<ReturnType>>

  setAuthToken(token: string): void

  registerRequestInterceptor(int: () => Promise<void>, order?: HandlerOrder): void
}
