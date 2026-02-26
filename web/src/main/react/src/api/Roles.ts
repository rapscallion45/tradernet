import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { List } from "api/types"

export type Role = {
  name: string
  resourceNames: string[]
}

export type UpdateRole = {
  resourceNames: string[]
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

  getResources(): List<string> {
    return this.typedSubPath<string>("resources")._list()
  }

  updateRole(name: string, payload: UpdateRole): Promise<Role> {
    return this.typedSubPath<Role, UpdateRole>(name)._update({ data: payload })
  }
}
