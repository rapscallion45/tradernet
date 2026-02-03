import React from "react"
import type { Meta, StoryObj } from "@storybook/react"
import { Card } from "./Card"
import { Group, Stack, Text, Title } from "@mantine/core"
import { IconMenu, IconUserCircle } from "@tabler/icons-react"
import { lengthControl, spacingControl, textOptions } from "../../storybook/mixins"

const meta = {
  title: "Tradernet/Card",
  component: Card,
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
} satisfies Meta<typeof Card>

export default meta
type Story = StoryObj<typeof Card>

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

// An example of how we could use the card to build more complex components
export const NoteCard: Story = {
  args: {
    padding: "lg",
    width: "50rem",
    children: (
      <Stack h={"100%"} justify={"space-between"}>
        <Group justify={"space-between"}>
          <Group>
            <IconUserCircle />
            <Title order={4}>A. NAME</Title>
          </Group>
          <Group>
            <Text>3:44 PM</Text>
            <IconMenu />
          </Group>
        </Group>

        <Text>{textOptions[2]}</Text>
      </Stack>
    ),
  },
}
