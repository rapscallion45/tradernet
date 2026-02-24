import React, { forwardRef, ReactNode } from "react"
import { Stack } from "@mantine/core"
import classes from "./BaseActionCard.module.css"
import { CommonCardProps, BaseCard } from "../BaseCard/BaseCard"

export type BaseActionCardProps = CommonCardProps & {
  topSection: ReactNode
  bottomSection?: ReactNode
}

/**
 * BaseActionCard is a base component for a **flexible** card that can be clicked to perform an action. It is designed for use within CardGrid.
 * It is built using BaseCard and used as the base for ActionCard and other similar components, and can also be used directly in projects
 * to provide common functionality and a consistent look and feel.
 *
 * @param topSection The content to display in the top section of the card. This is typically the main content of the card.
 * @param bottomSection Optional. The content to display in the bottom section of the card. This is typically secondary information or actions.
 *
 * @example
 * <BaseActionCard
 *   topSection={<Title order={3}>Card Title</Title>}
 *   bottomSection={<Text>Card Description</Text>}
 *   onClick={() => console.log("Card clicked")}
 * />
 *
 * @see BaseCard
 * @see ActionCard
 * @see StatCard
 * @see CardGrid
 */
export const BaseActionCard = forwardRef<HTMLButtonElement, BaseActionCardProps>(({ topSection, bottomSection, ...rest }, ref) => {
  return (
    <BaseCard ref={ref} classes={classes} {...rest}>
      <Stack h="100%" justify={"space-between"} gap={"xs"}>
        <div className={classes.topSection}>{topSection}</div>
        <div className={classes.bottomSection}>{bottomSection}</div>
      </Stack>
    </BaseCard>
  )
})
