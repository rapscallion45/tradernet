import { FC, ReactNode } from "react"
import { Card, Group, Stack, Text, Title } from "@mantine/core"
import classes from "./StatCard.module.css"

export type StatCardProps = {
  text: string
  icon?: ReactNode
  onClick?: () => void
  secondaryText?: string
  narrow?: boolean
}

/**
 *  StatCard component that displays a statistic with an action.
 *  The primary action is called by clicking the card itself, which can also have an icon that appears in the top right.
 *  The secondary text is displayed below the main text.
 */
export const StatCard: FC<StatCardProps> = ({ text, icon, onClick, secondaryText, narrow = false }) => (
  <Card w={narrow ? "18rem" : "22rem"} h={"8rem"} p={"md"} shadow={"xs"} radius={"sm"} onClick={onClick} aria-label={"stat-card"} data-clickable={!!onClick}>
    <Stack h={"100%"} justify={"space-between"} aria-roledescription={onClick ? "button" : "region"}>
      <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
        <Title order={2} className={classes.title}>
          {text}
        </Title>
        {!!icon}
      </Group>
      <Text className={classes.secondaryText}>{secondaryText}</Text>
    </Stack>
  </Card>
)
