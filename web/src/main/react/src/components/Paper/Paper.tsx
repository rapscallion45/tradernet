import { Paper as MantinePaper } from "@mantine/core"
import React, { FC, ReactNode } from "react"
import { AppLength, AppSpacing } from "global/types"
import classes from "./Paper.module.css"

export type PaperProps = {
  withBorder?: boolean
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
 * Paper component with set shadow and border radius.
 *
 * Padding can be controlled as well as height, width, and min and max values.
 * A border can be added if needed.
 */
export const Paper: FC<PaperProps> = ({ withBorder = false, padding = "md", height, minHeight, maxHeight, width, minWidth, maxWidth, children }) => {
  return (
    <MantinePaper
      // set in stone
      classNames={classes}
      shadow={"xs"}
      radius={"sm"}
      // set from props
      withBorder={withBorder}
      p={padding}
      h={height}
      mih={minHeight}
      mah={maxHeight}
      w={width}
      miw={minWidth}
      maw={maxWidth}>
      {children}
    </MantinePaper>
  )
}
