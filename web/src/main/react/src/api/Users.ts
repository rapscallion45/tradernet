import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { Get, List, User } from "api/types"

/**
 * Users resource definition
 */
export class UsersResource extends RestResource<User> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "users")
  }

  getUsers(): List<User> {
    return this._list()
  }

  getByUsername(username: string): Get<User> {
    return this.typedSubPath<User>("by-username", username)._get()
  }
}
