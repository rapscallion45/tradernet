import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { List } from "api/types"

export type Group = {
  id: number
}

/**
 * Groups resource definition
 */
export class GroupsResource extends RestResource<Group> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "groups")
  }

  getGroups(): List<Group> {
    return this._list()
  }
}
