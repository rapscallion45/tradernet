import { ClientAuthConfiguration } from "api/types"
import { ApiInterface } from "api/ApiInterface"
import ApiInterfaceAxios from "api/ApiInterfaceAxios"
import { AuthResource } from "api/Auth"

let client: RestClient | null = null

export function getRestClient(): RestClient {
  const viteApiBaseUrl = import.meta.env.VITE_SERVER_BASE_URL
  console.trace("VITE_SERVER_BASE_URL:", viteApiBaseUrl)

  if (!client) {
    const authConfiguration: ClientAuthConfiguration = {
      useCookies: true,
      useToken: false,
      tokenConfig: {
        asBearerToken: false,
        headerName: "",
      },
    }
    client = new RestClient(new ApiInterfaceAxios(viteApiBaseUrl, authConfiguration))
  }
  return client
}

export class RestClient {
  apiInterface: ApiInterface

  authResource: AuthResource

  constructor(apiInterface: ApiInterface) {
    this.apiInterface = apiInterface
    this.authResource = new AuthResource(this.apiInterface)
  }
}
