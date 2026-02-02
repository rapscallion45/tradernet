import { Flex, Notification, Progress, Stack, Text } from "@mantine/core"
import type { Meta, StoryObj } from "@storybook/react"
import React from "react"
import { icon } from "hooks/useToast"

const meta = {
  component: Notification,
  title: "Mantine/Notification",
  parameters: {
    layout: "centered",
  },
  argTypes: {},
  render: (args) => (
    // Slight hack to ensure Notifications render at a consistent width, like they do in the real container.
    <Flex w={"25rem"}>
      <Notification w={"100%"} {...args} />
    </Flex>
  ),
} satisfies Meta<typeof Notification>

export default meta

type Story = StoryObj<typeof Notification>

export const Default: Story = {
  args: {
    title: "Notification title",
    children: "Notification message",
  },
}

export const LongText: Story = {
  args: {
    title: "Notification title",
    children: "A super long message that you better pay attention to, buddy, or there'll be hell to pay!",
  },
}

export const MultipleLines: Story = {
  args: {
    title: "Notification title",
    children: (
      <Stack gap={"xs"}>
        <Text fz={"sm"}>A rather long message...</Text>
        <Text fz={"sm"}>... that is spread over two lines.</Text>
      </Stack>
    ),
  },
}

export const Info: Story = {
  args: {
    title: "Notification title",
    children: "Notification message",
    variant: "info",
  },
}

export const InfoLoading: Story = {
  args: {
    title: "Something is happening...",
    children: "Notification message",
    icon: icon("success"),
    loading: true,
    variant: "info",
  },
}

export const Success: Story = {
  args: {
    title: "Notification title",
    children: "Notification message",
    icon: icon("success"),
    variant: "success",
  },
}

export const SuccessLoading: Story = {
  args: {
    title: "Something is happening...",
    children: "Notification message",
    icon: icon("success"),
    variant: "success",
    loading: true,
  },
}

export const Error: Story = {
  args: {
    title: "Notification title",
    children: "Notification message",
    icon: icon("error"),
    variant: "error",
  },
}

export const ErrorLoading: Story = {
  args: {
    title: "Something is happening...",
    children: "Notification message",
    icon: icon("error"),
    variant: "error",
    loading: true,
  },
}

// A fairly simple example of a notification with a progress bar.
export const ProgressInfo: Story = {
  args: {
    variant: "info",
    title: "Notification title",
    children: (
      <Stack gap={"xs"}>
        <Text fz={"sm"}>This is the message...</Text>
        <Progress value={50} variant={"info"} />
      </Stack>
    ),
  },
}

export const ProgressSuccess: Story = {
  args: {
    variant: "success",
    title: "Notification title",
    children: (
      <Stack gap={"xs"}>
        <Text fz={"sm"}>This is the message...</Text>
        <Progress value={50} variant={"success"} />
      </Stack>
    ),
    icon: icon("success"),
  },
}

export const ProgressError: Story = {
  args: {
    variant: "error",
    title: "Notification title",
    children: (
      <Stack gap={"xs"}>
        <Text fz={"sm"}>This is the message...</Text>
        <Progress value={50} variant={"error"} />
      </Stack>
    ),
    icon: icon("error"),
  },
}
