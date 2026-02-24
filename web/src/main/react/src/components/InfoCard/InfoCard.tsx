import React, { FC, ReactNode, useId } from "react"
import { Group, Text, Title, Tooltip } from "@mantine/core"
import classes from "./InfoCard.module.css"
import { AppColor } from "global/types"
import { BaseActionCard } from "../BaseActionCard/BaseActionCard"
import { kebabCase } from "utils/strings"

export type InfoCardProps = {
  text: string
  secondaryText?: string
  icon?: ReactNode
  color?: AppColor
  tooltip?: string
  onClick?: () => void
}

/**
 * InfoCard component to display static information
 *
 * @param text Primary piece of information to be displayed. This will be larger than the secondaryText and colour matches the UI.
 * @param secondaryText Optional. Accompanying piece of information displayed beneath primary text. Use for a short description, contextual information etc.
 * @param icon Optional. Icon to render in top right of card
 * @param color Optional. The colour of the primary text.
 * @param tooltipText Optional. Displays longer pieces of information that don't fit within the card. If provided will show tooltip with circle-info icon.
 * @param onClick Optional. On click event of card
 */
export const InfoCard: FC<InfoCardProps> = ({ text, secondaryText, tooltip, icon, color, onClick }) => {
  const autoId = useId()
  // Assign unique ids for the label and value for accessibility
  const labelId = `info-label-${kebabCase(secondaryText || "")}-${autoId}`
  const valueId = `info-value-${kebabCase(text)}-${autoId}`

  return (
    <Tooltip label={tooltip} multiline withArrow disabled={!tooltip} maw={350}>
      <BaseActionCard
        data-testid={`InfoCard-${text}`}
        aria-labelledby={`${labelId} ${valueId}`}
        onClick={onClick}
        topSection={
          <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
            <Title id={valueId} order={2} className={classes.title} c={color}>
              {text}
            </Title>
            {icon}
          </Group>
        }
        bottomSection={
          <Text className={classes.secondaryText} id={labelId}>
            {secondaryText}
          </Text>
        }
      />
    </Tooltip>
  )
}
