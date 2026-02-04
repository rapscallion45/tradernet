import { ApiInterface, ApiRequestType } from "api/ApiInterface"
import { ApiRequest, ApiRequestWithBaseUrl, ApiResponse, BaseApiRequest, Get, List } from "api/types"
import { buildUrl } from "./util"

export class RestResource<ReturnType, PayloadType = ReturnType> {
  public resourcePath: string
  protected apiInterface: ApiInterface

  constructor(apiInterface: ApiInterface, resourcePath?: string) {
    this.apiInterface = apiInterface
    this.resourcePath = resourcePath ?? ""
  }

  protected static noOp = (apiResponse: unknown, logIt = false): void => {
    if (logIt) console.trace(apiResponse)
  }

  protected forType<ForReturnType, ForPayloadType = ForReturnType>() {
    return new SubResource<ForReturnType, ForPayloadType>(this.apiInterface, this.resourcePath)
  }

  protected typedSubPath<ForReturnType, ForPayloadType = ForReturnType>(...subPath: string[]) {
    return new SubResource<ForReturnType, ForPayloadType>(this.apiInterface, buildUrl(this.resourcePath, ...subPath))
  }

  protected subPath(...subPath: string[]) {
    return this.typedSubPath<ReturnType, PayloadType>(...subPath)
  }

  protected _request(type: ApiRequestType, config?: ApiRequest<PayloadType>): Get<ApiResponse<ReturnType>> {
    return this.apiInterface.fetch<ReturnType, PayloadType>(type, this.addUrlToConfig(config))
  }

  protected _create(config: ApiRequest<PayloadType>): Get<ReturnType> {
    return this._request(ApiRequestType.Create, config).then(this.getData)
  }

  protected _update(config: ApiRequest<PayloadType>): Get<ReturnType> {
    return this._request(ApiRequestType.Update, config).then(this.getData)
  }

  protected _delete(config: ApiRequest<PayloadType>): Get<ReturnType> {
    return this._request(ApiRequestType.Delete, config).then(this.getData)
  }

  protected _get(config?: BaseApiRequest): Get<ReturnType> {
    return this._request(ApiRequestType.Get, config).then(this.getData)
  }

  protected _list(config?: BaseApiRequest): List<ReturnType> {
    return this.forType<Array<ReturnType>>()._request(ApiRequestType.Get, config).then(this.getData)
  }

  protected addUrlToConfig<T>(config?: ApiRequest<T>): ApiRequestWithBaseUrl<T> {
    return {
      ...(config ?? {}),
      resourcePath: this.resourcePath,
    }
  }

  private getData<T>(resp: ApiResponse<T>): T {
    return resp.data
  }
}

export class SubResource<ReturnType, PayloadType = ReturnType> extends RestResource<ReturnType, PayloadType> {
  public forType<ForReturnType, ForPayloadType = ForReturnType>() {
    return super.forType<ForReturnType, ForPayloadType>()
  }

  public _request(type: ApiRequestType, config?: ApiRequest<PayloadType>): Get<ApiResponse<ReturnType>> {
    return super._request(type, config)
  }

  _create(config: ApiRequest<PayloadType>): Get<ReturnType> {
    return super._create(config)
  }

  public _update(config: ApiRequest<PayloadType>): Get<ReturnType> {
    return super._update(config)
  }

  public _delete(config: ApiRequest<PayloadType>): Get<ReturnType> {
    return super._delete(config)
  }

  public _get(config?: BaseApiRequest): Get<ReturnType> {
    return super._get(config)
  }

  public _list(config?: BaseApiRequest): List<ReturnType> {
    return super._list(config)
  }
}
