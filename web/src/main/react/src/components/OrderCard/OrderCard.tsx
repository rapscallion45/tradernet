import { forwardRef } from "react"
import { Stack, Title } from "@mantine/core"
import classes from "./OrderCard.module.css"
import { BaseCard } from "../BaseCard/BaseCard"
import OrderForm from "../../pages/Dashboard/forms/OrderForm"
import { OrderData } from "api/types"
import { useCreateOrder } from "hooks/useOrders"
import { useToast } from "hooks/useToast"

/**
 * Order Card component
 */
export const OrderCard = forwardRef<HTMLButtonElement>(({ ...rest }, ref) => {
  const createOrder = useCreateOrder()
  const { toast } = useToast()

  const handleSubmit = async (data: OrderData) => {
    try {
      await createOrder.mutateAsync(data)
      toast({
        id: "create-order-success",
        variant: "success",
        title: "Order submitted",
        message: `Submitted ${data.side} ${data.quantity} ${data.symbol.toUpperCase()} @ ${data.price}`,
        timestamp: Date.now(),
      })
    } catch {
      toast({
        id: "create-order-fail",
        variant: "error",
        title: "Failed to submit order",
        message: "Please check your input and try again.",
        timestamp: Date.now(),
      })
    }
  }

  return (
    <BaseCard ref={ref} classes={classes} {...rest}>
      <Stack h="100%" justify={"space-between"} gap={"xs"}>
        <div className={classes.topSection}>
          <Title order={3}>New Order</Title>
        </div>
        <div className={classes.bottomSection}>
          <OrderForm onSubmit={handleSubmit} loading={createOrder.isPending} />
        </div>
      </Stack>
    </BaseCard>
  )
})
