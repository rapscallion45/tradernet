import type { Meta, StoryObj } from "@storybook/react"
import { Textarea } from "@mantine/core"

const meta = {
  title: "Mantine/Textarea",
  component: Textarea,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <Textarea {...args} />
    </div>
  ),
} satisfies Meta<typeof Textarea>

export default meta
type Story = StoryObj<typeof Textarea>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet Text Area",
  },
}

/**
 *  Autosize allows the textarea to grow as the user types
 */
export const Autosize: Story = {
  args: {
    label: "Tradernet Text Area",
    size: "sm",
    rows: 1,
    maxRows: 5,
    autosize: true,
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet Text Area",
    value: "Tradernet Text Area Disabled",
    disabled: true,
  },
}

/**
 *  Placeholder story
 */
export const Placeholder: Story = {
  args: {
    label: "Tradernet Text Area",
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
    placeholder: "Test placeholder...",
    label: "Tradernet Text Area",
  },
}

/**
 *  With Description story
 */
export const WithDescription: Story = {
  args: {
    placeholder: "Test placeholder...",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescription: Story = {
  args: {
    label: "Tradernet Text Area",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    label: "Tradernet Text Area",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
