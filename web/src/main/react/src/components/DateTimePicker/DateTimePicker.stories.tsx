import type { Meta, StoryObj } from "@storybook/react"
import { DateTimePicker } from "@mantine/dates"

const meta = {
  title: "Mantine/DateTimePicker",
  component: DateTimePicker,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <DateTimePicker {...args} />
    </div>
  ),
} satisfies Meta<typeof DateTimePicker>

export default meta
type Story = StoryObj<typeof DateTimePicker>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet Date Time Picker",
    value: new Date(2001, 0, 1, 12, 30, 0),
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet Date Time Picker",
    value: new Date(2001, 0, 1, 12, 30, 0),
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
    label: "Tradernet Date Time Picker",
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
    label: "Tradernet Date Time Picker",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    label: "Tradernet Date Time Picker",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
