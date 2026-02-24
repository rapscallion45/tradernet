import React, { FC, ReactNode } from "react"
import { Group } from "@mantine/core"

/**
 * CheckboxCardGroup component to layout child checkbox cards in rows, ensuring their height matches.
 *
 * @param children - The components to be displayed in the grid
 */
export const CheckboxCardGroup: FC<{ children: ReactNode }> = ({ children }) => <Group align={"stretch"}>{children}</Group>
