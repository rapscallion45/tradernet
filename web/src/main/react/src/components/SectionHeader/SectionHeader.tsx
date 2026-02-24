import React, { ReactNode, FC } from "react"
import { Divider, Flex, Group, Text } from "@mantine/core"

type SectionHeaderProps = {
  description?: string
  leftSection?: ReactNode[]
  rightSection?: ReactNode[]
}

/**
 * SectionHeader is a basic component for use in places like Tabs, where various smaller components are needed for secondary actions, such as buttons and filters.
 * It gives a cleaner way to handle the layout, ensuring surrounding Group/Stack components have consistent sizes etc.
 *
 * @param description The line of text to describe the section. Should be passed via a Mantine <Text></Text> component.
 * @param leftSection The elements to appear in the left of the header. Passed as an array of ReactNodes.
 * @param rightSection The elements to appear in the right of the header. Passed as an array of ReactNodes.
 */
export const SectionHeader: FC<SectionHeaderProps> = ({ description, leftSection, rightSection }) => {
  return (
    <Group justify={"space-between"} align={"center"} wrap={"nowrap"}>
      <Flex direction={"row"} gap={"md"} align={"center"}>
        {description && <Text>{description}</Text>}
        {leftSection && (
          <Group justify={"stretch"} wrap={"nowrap"}>
            {description && <Divider orientation="vertical" />}
            {leftSection}
          </Group>
        )}
      </Flex>
      {rightSection && (
        <Group align={"center"} wrap={"nowrap"}>
          {rightSection}
        </Group>
      )}
    </Group>
  )
}
