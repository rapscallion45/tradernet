import { forwardRef } from "react"
import { Stack, Title } from "@mantine/core"
import classes from "./OrderCard.module.css"
import { BaseCard } from "../BaseCard/BaseCard"
import OrderForm from "../../pages/Dashboard/forms/OrderForm"
import { OrderData } from "api/types"
import { useCreateOrder } from "hooks/useCreateOrder"

/**
 * Order Card component
 */
export const OrderCard = forwardRef<HTMLButtonElement>(({ ...rest }, ref) => {
  const createOrder = useCreateOrder()

  const handleSubmit = (data: OrderData) => {
    createOrder.mutate(data)
  }

  return (
    <BaseCard ref={ref} classes={classes} {...rest}>
      <Stack justify={"space-between"} gap={"md"}>
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
