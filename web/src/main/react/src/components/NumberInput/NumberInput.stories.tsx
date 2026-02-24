import type { Meta, StoryObj } from "@storybook/react"
import { expect, userEvent, within } from "@storybook/test"
import { NumberInput } from "@mantine/core"

const meta = {
  title: "Mantine/NumberInput",
  component: NumberInput,
  parameters: {
    layout: "centered",
  },
  render: (args) => (
    <div style={{ width: "300px" }}>
      <NumberInput {...args} />
    </div>
  ),
} satisfies Meta<typeof NumberInput>

export default meta
type Story = StoryObj<typeof NumberInput>

/**
 *  Default story
 */
export const Default: Story = {
  args: {
    label: "Tradernet Number Input",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const input = canvas.getByLabelText("Tradernet Number Input", {
      selector: "input",
    })

    await userEvent.type(input, "12345", {
      delay: 100,
    })

    /** Input should have updated */
    expect(input).toHaveDisplayValue("12345")
  },
}

export const Required: Story = {
  args: {
    label: "Tradernet Number Input",
    required: true,
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    label: "Tradernet Number Input",
    value: "Tradernet Number Input Disabled",
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
    label: "Tradernet Number Input",
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
    label: "Tradernet Number Input",
    description: "This is a test description sentence.",
  },
}

export const WithLabelAndDescriptionAndError: Story = {
  args: {
    placeholder: "Test placeholder...",
    label: "Tradernet Number Input",
    description: "This is a test description sentence.",
    error: "This is a test error message",
  },
}
