import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { List } from "api/types"

export type Role = {
  name: string
}

/**
 * Roles resource definition
 */
export class RolesResource extends RestResource<Role> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "roles")
  }

  getRoles(): List<Role> {
    return this._list()
  }
}
