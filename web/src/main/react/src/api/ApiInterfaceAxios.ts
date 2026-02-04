import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, Method } from "axios"
import { ApiInterface, ApiRequestType, HandlerOrder, InterceptorHandler } from "api/ApiInterface"
import { ApiRequestWithBaseUrl, ApiResponse, ClientAuthConfiguration, GlobalErrorCallback } from "api/types"

const constructAxiosInstance = (baseURL: string) => {
  /** axios client config */
  const axiosBaseConfig = {
    baseURL: `/tradernet`,
    paramsSerializer: {
      encode: (param: string | number | boolean) => encodeURIComponent(param),
    },
  }

  return axios.create({
    ...axiosBaseConfig,
  })
}

/**
 * Axios API Client definition
 */
class ApiInterfaceAxios implements ApiInterface {
  public onError
  public axiosInstance: AxiosInstance
  private interceptors: Array<InterceptorHandler> = []
  private authToken: string | null = null

  constructor(serverUrl: string, authConfiguration: ClientAuthConfiguration, onError?: GlobalErrorCallback) {
    const axiosInstance = constructAxiosInstance(serverUrl)
    console.log("Axios instance constructed with base URL:", axiosInstance.defaults.baseURL)
    this.axiosInstance = axiosInstance
    this.onError = onError

    if (authConfiguration.useToken) {
      const tokenHeader = authConfiguration.tokenConfig.asBearerToken ? "Authorization" : authConfiguration.tokenConfig.headerName
      this.axiosInstance.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
        for (const interceptor of this.interceptors) {
          if (interceptor && interceptor.order === HandlerOrder.PRE_AUTH) {
            await interceptor.handler()
          }
        }

        if (this.authToken) {
          config.headers[tokenHeader] = tokenHeader === "Authorization" ? "Bearer " + this.authToken : this.authToken
        }

        for (const interceptor of this.interceptors) {
          if (interceptor && interceptor.order === HandlerOrder.POST_AUTH) {
            await interceptor.handler()
          }
        }
        return config
      })
    }
  }

  /** Register an interceptor. CAUTION - this will fire on *every* API call so should avoid expensive computation and not block */
  public registerRequestInterceptor = (int: () => Promise<void>, order?: HandlerOrder): number => {
    this.interceptors.push({ handler: int, order: order ? order : HandlerOrder.POST_AUTH })
    return this.interceptors.length - 1
  }

  public setAuthToken = (token: string) => {
    this.authToken = token
  }

  public fetch<ReturnType, PayloadType>(type: ApiRequestType, config: ApiRequestWithBaseUrl<PayloadType>): Promise<ApiResponse<ReturnType>> {
    return this.axiosInstance
      .request<ReturnType>({
        method: this.requestTypeToAxiosMethod(type),
        url: buildUrl(config.resourcePath, ...(config.pathParams?.flatMap((pp) => `${pp}`) ?? [])),
        params: config.queryParams,
        headers: config.headers,
        data: config.data,
        ...config.bespokeConfig?.axios,
      })
      .then((value: AxiosResponse<ReturnType>) => {
        if (config.validateStatus && !config.validateStatus(value.status, value)) throw value

        return {
          data: value.data,
          headers: value.headers as unknown,
          status: value.status,
          statusText: value.statusText,
        }
      })
      .catch((err) => {
        if (this.onError) this.onError(err, config)

        throw err
      })
  }

  private requestTypeToAxiosMethod = (type: ApiRequestType): Method => {
    switch (type) {
      case ApiRequestType.Get:
        return "get"
      case ApiRequestType.Create:
        return "post"
      case ApiRequestType.Update:
        return "put"
      case ApiRequestType.Delete:
        return "delete"
    }
  }
}

export function stripStartAndEndSlash(str: string): string {
  const stripStart = str.startsWith("/")
  const stripEnd = str.endsWith("/")
  return str.slice(stripStart ? 1 : 0, stripEnd ? -1 : undefined)
}

export function buildUrl(...parts: string[]): string {
  return parts.map(stripStartAndEndSlash).join("/")
}

export default ApiInterfaceAxios
