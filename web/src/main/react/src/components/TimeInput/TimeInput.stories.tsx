import type { Meta, StoryObj } from "@storybook/react"
import { TimeInput } from "@mantine/dates"

const meta = {
  title: "Mantine/TimeInput",
  component: TimeInput,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <TimeInput {...args} />
    </div>
  ),
} satisfies Meta<typeof TimeInput>

export default meta
type Story = StoryObj<typeof TimeInput>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet Time Input",
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet Time Input",
    disabled: true,
  },
}

/**
 *  Placeholder story
 */
export const Placeholder: Story = {
  args: {
    placeholder: "Test placeholder...",
  },
}

/**
 *  Error story
 */
export const Error: Story = {
  args: {
    placeholder: "Test placeholder...",
    error: "This is a test error message",
  },
}

/**
 *  With Label story
 */
export const WithLabel: Story = {
  args: {
    label: "Tradernet Time Input",
  },
}

/**
 *  With Description story
 */
export const WithDescription: Story = {
  args: {
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescription: Story = {
  args: {
    label: "Tradernet Time Input",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    label: "Tradernet Time Input",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
