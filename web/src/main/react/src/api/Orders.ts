import { ApiInterface } from "api/ApiInterface"
import { RestResource } from "api/RestResource"
import { List, OrderData, OrderSide, OrderSummary } from "api/types"

export class OrdersResource extends RestResource<OrderSummary, OrderData> {
  constructor(apiInterface: ApiInterface) {
    super(apiInterface, "orders")
  }

  getOrders(userId?: number, currency?: string): List<OrderSummary> {
    const queryParams: Record<string, string | number> = {}
    if (userId) queryParams.userId = userId
    if (currency) queryParams.currency = currency

    return this._list(Object.keys(queryParams).length > 0 ? { queryParams } : undefined)
  }

  createOrder(payload: OrderData): Promise<OrderSummary> {
    return this._create({ data: payload })
  }

  closeOrder(orderId: number): Promise<OrderSummary> {
    return this.subPath(String(orderId), "close")._update({})
  }
}

export const ORDER_SIDES: OrderSide[] = ["BUY", "SELL"]
