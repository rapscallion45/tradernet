import { FC, ReactNode } from "react"
import { Card as MantineCard } from "@mantine/core"
import { AppLength, AppSpacing } from "global/types"

export type CardProps = {
  padding?: AppSpacing
  width?: AppLength
  minWidth?: AppLength
  maxWidth?: AppLength
  height?: AppLength
  minHeight?: AppLength
  maxHeight?: AppLength
  children: ReactNode
}

/**
 * Card component with set shadow and border radius. Padding can be controlled as well as height, width, and min and max values.
 */
export const Card: FC<CardProps> = ({ padding = "md", height, minHeight, maxHeight, width, minWidth, maxWidth, children }) => {
  return (
    <MantineCard shadow={"xs"} radius={"sm"} p={padding} h={height} mih={minHeight} mah={maxHeight} w={width} miw={minWidth} maw={maxWidth}>
      {children}
    </MantineCard>
  )
}
