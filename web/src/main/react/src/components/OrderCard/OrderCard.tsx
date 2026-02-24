import React, { forwardRef, ReactNode } from "react"
import { Stack, Title } from "@mantine/core"
import classes from "./OrderCard.module.css"
import { BaseCard } from "../BaseCard/BaseCard"
import OrderForm from "../../pages/Dashboard/forms/OrderForm"

/**
 * Order Card component
 */
export const OrderCard = forwardRef<HTMLButtonElement>(({ ...rest }, ref) => {
  return (
    <BaseCard ref={ref} classes={classes} {...rest}>
      <Stack h="100%" justify={"space-between"} gap={"xs"}>
        <div className={classes.topSection}>
          <Title order={3}>New Order</Title>
        </div>
        <div className={classes.bottomSection}>
          <OrderForm onSubmit={() => {}} />
        </div>
      </Stack>
    </BaseCard>
  )
})
