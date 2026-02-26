import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { List } from "api/types"

export type Group = {
  id: number
  name?: string
  usernames: string[]
  roleNames: string[]
}

export type UpdateGroup = {
  usernames: string[]
  roleNames: string[]
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

  updateGroup(id: number, payload: UpdateGroup): Promise<Group> {
    return this.typedSubPath<Group, UpdateGroup>(`${id}`)._update({ data: payload })
  }
}
