import React, { FC, ReactNode } from "react"
import { Text } from "@mantine/core"

/**
 * SectionHeading component for displaying section headings with consistent styling.
 * @param children The text to display. We use children as it's more natural for text content.
 */
export const SectionHeading: FC<{ children: ReactNode }> = ({ children }) => (
  <Text size={"sm"} fw={"bold"} styles={{ root: { letterSpacing: "0.1rem", textTransform: "uppercase" } }}>
    {children}
  </Text>
)
