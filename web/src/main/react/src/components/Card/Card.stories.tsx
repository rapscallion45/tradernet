import type { Meta, StoryObj } from "@storybook/react"
import React from "react"
import { Card, Group, Stack, Text, Title } from "@mantine/core"
import { lengthControl, spacingControl, textOptions } from "../../../storybook/mixins"
import { Icon } from "../Icon/Icon"

const meta = {
  title: "Mantine/Card",
  component: Card,
  parameters: {
    layout: "centered",
  },
  argTypes: {
    p: spacingControl,
    h: lengthControl,
    mih: lengthControl,
    mah: lengthControl,
    w: lengthControl,
    miw: lengthControl,
    maw: lengthControl,
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
    p: "sm",
  },
}

export const ExtraLargePadding: Story = {
  args: {
    p: "xxl",
  },
}

export const CustomHeight: Story = {
  args: {
    h: "10rem",
    children: (
      <Stack h={"100%"} justify={"center"}>
        {textOptions[1]}
      </Stack>
    ),
  },
}

export const CustomWidth: Story = {
  args: {
    w: "40rem",
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
    p: "lg",
    w: "50rem",
    children: (
      <Stack h="100%" justify={"space-between"}>
        <Group justify={"space-between"}>
          <Group>
            <Icon name={"user"} />
            <Title order={4}>A. NAME</Title>
          </Group>
          <Group>
            <Text>3:44 PM</Text>
            <Icon name={"ellipsis-vertical"} />
          </Group>
        </Group>
        <Text>{textOptions[2]}</Text>
      </Stack>
    ),
  },
}
