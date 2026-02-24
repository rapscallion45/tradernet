import type { Meta, StoryObj } from "@storybook/react"
import { DatePickerInput } from "@mantine/dates"

const meta = {
  title: "Mantine/DatePickerInput",
  component: DatePickerInput,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <DatePickerInput {...args} />
    </div>
  ),
} satisfies Meta<typeof DatePickerInput>

export default meta
type Story = StoryObj<typeof DatePickerInput>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet Date Picker",
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet Date Picker",
    value: new Date("01-01-2001"),
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
    label: "Tradernet Date Picker",
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
    label: "Tradernet Date Picker",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    label: "Tradernet Date Picker",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
