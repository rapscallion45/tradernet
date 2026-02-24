import React, { FC, ReactNode } from "react"
import { SimpleGrid } from "@mantine/core"

/**
 * CardGrid component to layout child cards in a responsive grid.
 * The grid adjusts the number of columns based on the **container** width.
 *
 * @param children - The card components to be displayed in the grid
 */
export const CardGrid: FC<{ children: ReactNode }> = ({ children }) => (
  <SimpleGrid cols={{ base: 1, "48rem": 2, "72rem": 3, "96rem": 4, "120rem": 5, "144rem": 6, "168rem": 7, "192rem": 8 }} type={"container"}>
    {children}
  </SimpleGrid>
)
