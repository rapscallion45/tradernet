import React, { FC, ReactNode, useId } from "react"
import { Group, Text, Title, Tooltip } from "@mantine/core"
import classes from "./ActionCard.module.css"
import { CommonCardProps } from "../BaseCard/BaseCard"
import { BaseActionCard } from "../BaseActionCard/BaseActionCard"
import { kebabCase } from "utils/strings"

export type ActionCardProps = CommonCardProps & {
  text: string
  icon?: ReactNode
  tooltip?: string
  secondaryText?: string
}

/**
 * ActionCard component with a primary action and list of secondary actions.
 * The primary action is called by clicking the card itself, which can also have an icon that appears in the top right.
 * The secondary actions are displayed in a kebab menu in the bottom right of the card.
 * The card incorporates all the features of BaseCard, including featured, disabled, and modified states.
 *
 * @param text The main text to display in the card.
 * @param icon Optional. The icon to display in the top right of the card.
 * @param tooltip Optional. The tooltip to display when hovering over the icon.
 * @param secondaryText Optional. The secondary text to display below the main text.
 */
export const ActionCard: FC<ActionCardProps> = ({ text, icon, tooltip, secondaryText, ...rest }) => {
  const autoId = useId()
  // Assign unique ids for the label and value for accessibility
  const labelId = `action-label-${kebabCase(text)}-${autoId}`

  return (
    <BaseActionCard
      data-testid={`ActionCard-${text}`}
      aria-labelledby={labelId}
      {...rest}
      topSection={
        <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
          <Title order={3} className={classes.title} id={labelId}>
            {text}
          </Title>
          {icon && (
            <Tooltip label={tooltip} disabled={!tooltip}>
              {icon}
            </Tooltip>
          )}
        </Group>
      }
      bottomSection={
        <Group justify={"space-between"} data-testid={`ActionCard-${text}-Secondary-Section`} wrap={"nowrap"}>
          <Text className={classes.secondaryText}>{secondaryText}</Text>
        </Group>
      }
    />
  )
}
