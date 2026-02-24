import type { Meta, StoryObj } from "@storybook/react"
import { expect, userEvent, within } from "@storybook/test"
import { FileInput } from "@mantine/core"

const meta = {
  title: "Mantine/FileInput",
  component: FileInput,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <FileInput {...args} />
    </div>
  ),
} satisfies Meta<typeof FileInput>

export default meta
type Story = StoryObj<typeof FileInput>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet File Input",
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet File Input",
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
    placeholder: "Test placeholder...",
    label: "Tradernet File Input",
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
    placeholder: "Test placeholder...",
    label: "Tradernet File Input",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    placeholder: "Test placeholder...",
    label: "Tradernet File Input",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
