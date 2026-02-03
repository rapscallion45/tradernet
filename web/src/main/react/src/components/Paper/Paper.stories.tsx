import React, { ReactNode } from "react"
import type { Meta, StoryObj } from "@storybook/react"
import { Paper } from "./Paper"
import { Flex, Group, Stack, Text } from "@mantine/core"
import { lengthControl, spacingControl, textOptions } from "../../storybook/mixins"

/** Mock child component */
const MockChild: ReactNode = (
  <Flex justify={"center"} p={10} direction={"column"}>
    <Text size={"xl"}>Tradernet Paper</Text>
    <Text size={"sm"}>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</Text>
  </Flex>
)

const meta = {
  title: "Tradernet/Paper",
  component: Paper,
  parameters: {
    layout: "centered",
  },
  argTypes: {
    padding: spacingControl,
    height: lengthControl,
    minHeight: lengthControl,
    maxHeight: lengthControl,
    width: lengthControl,
    minWidth: lengthControl,
    maxWidth: lengthControl,
    children: {
      options: textOptions,
    },
  },
  args: {
    children: textOptions[0],
  },
} satisfies Meta<typeof Paper>

export default meta
type Story = StoryObj<typeof Paper>

export const Default: Story = {}

export const SmallPadding: Story = {
  args: {
    padding: "sm",
  },
}

export const ExtraLargePadding: Story = {
  args: {
    padding: "xxl",
  },
}

export const CustomHeight: Story = {
  args: {
    height: "10rem",
    children: (
      <Stack h={"100%"} justify={"center"}>
        {textOptions[1]}
      </Stack>
    ),
  },
}

export const CustomWidth: Story = {
  args: {
    width: "40rem",
    children: (
      <Group w={"100%"} justify={"flex-end"}>
        {textOptions[1]}
      </Group>
    ),
  },
}

export const WithChild: Story = {
  args: {
    children: MockChild,
  },
}
