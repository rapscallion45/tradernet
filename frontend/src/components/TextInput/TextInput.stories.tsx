import type { Meta, StoryObj } from "@storybook/react"
import { expect, userEvent, within } from "@storybook/test"
import { TextInput } from "@mantine/core"

const meta = {
  title: "Mantine/TextInput",
  component: TextInput,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <TextInput {...args} />
    </div>
  ),
} satisfies Meta<typeof TextInput>

export default meta
type Story = StoryObj<typeof TextInput>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet Text Input",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const emailInput = canvas.getByLabelText("Tradernet Text Input", {
      selector: "input",
    })

    await userEvent.type(emailInput, "Tradernet Text Input Default", {
      delay: 100,
    })

    /** Input should have updated */
    expect(emailInput).toHaveDisplayValue("Tradernet Text Input Default")
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet Text Input",
    value: "Tradernet Text Input Disabled",
    disabled: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const emailInput = canvas.getByLabelText("Tradernet Text Input", {
      selector: "input",
    })

    await userEvent.type(emailInput, "Tradernet Text Input Default", {
      delay: 100,
    })

    /** Input should not have updated as it is disabled */
    expect(emailInput).toHaveDisplayValue("Tradernet Text Input Disabled")
  },
}

/**
 *  Placeholder story
 */
export const Placeholder: Story = {
  args: {
    placeholder: "Test placeholder...",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const emailInput = canvas.getByPlaceholderText("Test placeholder...")

    /** Input value should be null, therefore showing placeholder instead */
    expect(emailInput).toHaveDisplayValue("")
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
    label: "Tradernet Text Input",
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
    label: "Tradernet Text Input",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    placeholder: "Test placeholder...",
    label: "Tradernet Text Input",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
