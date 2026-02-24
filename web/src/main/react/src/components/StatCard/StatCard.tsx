import React, { FC, ReactNode, useId } from "react"
import { Group, Text, Title } from "@mantine/core"
import classes from "./StatCard.module.css"
import { kebabCase } from "utils/strings"
import { BaseActionCard } from "../BaseActionCard/BaseActionCard"

export type StatCardProps = {
  text: string
  secondaryText?: string
  icon?: ReactNode
  onClick?: () => void
}

/**
 *  StatCard component that displays a statistic with an action.
 *  The primary action is called by clicking the card itself, which can also have an icon that appears in the top right.
 *  The secondary text is displayed below the main text.
 */
export const StatCard: FC<StatCardProps> = ({ text, icon, onClick, secondaryText }) => {
  const autoId = useId()
  // Assign unique ids for the label and value for accessibility
  const labelId = `stat-label-${kebabCase(secondaryText || "")}-${autoId}`
  const valueId = `stat-value-${kebabCase(text)}-${autoId}`

  return (
    <BaseActionCard
      data-testid={`StatCard-${text}`}
      aria-labelledby={`${labelId} ${valueId}`}
      onClick={onClick}
      topSection={
        <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
          <Title id={valueId} order={2} className={classes.title}>
            {text}
          </Title>
          {icon}
        </Group>
      }
      bottomSection={<Text id={labelId}>{secondaryText}</Text>}
    />
  )
}
