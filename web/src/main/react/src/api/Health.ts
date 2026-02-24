import { RestResource } from "api/RestResource"
import { ApiInterface } from "api/ApiInterface"
import { Get, HealthResponse } from "api/types"

/**
 * Health resource definition
 */
export class HealthResource extends RestResource<HealthResponse, void> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "health")
  }

  get(): Get<HealthResponse> {
    return this.typedSubPath<HealthResponse>()._get()
  }
}
