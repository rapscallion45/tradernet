import React, { FC, ReactNode } from "react"
import { Card, Group, Stack, Text, Title, Tooltip } from "@mantine/core"
import classes from "./ActionCard.module.css"

export type ActionCardProps = {
  text: string
  icon?: ReactNode
  action?: () => void
  tooltip?: string
  secondaryText?: string
  secondaryIcon?: ReactNode
  secondaryToolTip?: string
  featured?: boolean
  disabled?: boolean
  modified?: boolean
  narrow?: boolean
}

/**
 *  ActionCard component with a primary and secondary action.
 *  The primary action is called by clicking the card itself, which can also have an icon that appears in the top right.
 *  The secondary action is a clickable icon that appears in the bottom right.
 *  The card can be disabled or featured.
 */
export const ActionCard: FC<ActionCardProps> = ({
  text,
  icon,
  action,
  tooltip,
  secondaryText,
  secondaryIcon,
  secondaryToolTip,
  featured = false,
  disabled = false,
  modified = false,
  narrow = false,
}) => (
  <Card
    mod={{ modified }}
    w={narrow ? "18rem" : "22rem"}
    h={"8rem"}
    p={"md"}
    shadow={"xs"}
    radius={"sm"}
    className={classes.card}
    classNames={{ root: disabled ? classes.disabled : featured ? classes.featured : classes.regular }}
    data-testid={`ActionCard-${text}`}
    onClick={!disabled ? action : undefined}>
    <Stack h={"100%"} justify={"space-between"}>
      <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
        <Title order={3} className={classes.title}>
          {text}
        </Title>
        {icon && (
          <Tooltip label={tooltip} disabled={!tooltip} withArrow>
            {icon}
          </Tooltip>
        )}
      </Group>
      {!disabled && (
        <Group className={classes.secondarySection} justify={"space-between"} data-testid={`ActionCard-${text}-Secondary-Section`}>
          <Text className={classes.secondaryText}>{secondaryText}</Text>
          {secondaryIcon && (
            <Tooltip label={secondaryToolTip} disabled={!secondaryToolTip} position={"top"} withArrow>
              {secondaryIcon}
            </Tooltip>
          )}
        </Group>
      )}
    </Stack>
  </Card>
)
