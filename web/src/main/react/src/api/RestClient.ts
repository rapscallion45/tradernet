import { ClientAuthConfiguration } from "api/types"
import { ApiInterface } from "api/ApiInterface"
import ApiInterfaceAxios from "api/ApiInterfaceAxios"
import { AuthResource } from "api/Auth"
import { GroupsResource } from "api/Groups"
import { HealthResource } from "api/Health"
import { RolesResource } from "api/Roles"
import { UsersResource } from "api/Users"
import { OrdersResource } from "api/Orders"
import { MarketResource } from "api/Market"
import { PortfolioResource } from "api/Portfolio"

let client: RestClient | null = null

export function getRestClient(): RestClient {
  const viteApiBaseUrl = import.meta.env.VITE_SERVER_BASE_URL || "/api"
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
  healthResource: HealthResource
  groupsResource: GroupsResource
  rolesResource: RolesResource
  usersResource: UsersResource
  ordersResource: OrdersResource
  marketResource: MarketResource
  portfolioResource: PortfolioResource

  constructor(apiInterface: ApiInterface) {
    this.apiInterface = apiInterface
    this.authResource = new AuthResource(this.apiInterface)
    this.healthResource = new HealthResource(this.apiInterface)
    this.groupsResource = new GroupsResource(this.apiInterface)
    this.rolesResource = new RolesResource(this.apiInterface)
    this.usersResource = new UsersResource(this.apiInterface)
    this.ordersResource = new OrdersResource(this.apiInterface)
    this.marketResource = new MarketResource(this.apiInterface)
    this.portfolioResource = new PortfolioResource(this.apiInterface)
  }
}
