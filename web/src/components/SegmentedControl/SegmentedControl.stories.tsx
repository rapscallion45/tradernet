import { SegmentedControl } from "@mantine/core"
import { Meta, StoryObj } from "@storybook/react"
import { expect, waitFor, within } from "@storybook/test"

/**
 * SegmentedControl is showcased here with some simple examples.
 *
 * We do not have our own SegmentedControl component as it is not needed. Simply using the Mantine SegmentedControl will use the
 * correct styles, as set in the theme.
 */
const meta = {
  component: SegmentedControl,
  title: "Mantine/SegmentedControl",
  parameters: {
    layout: "padded",
  },
  args: {
    data: ["One", "Two", "Three"],
  },
  argTypes: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    await waitFor(() => {
      // Hack to ensure the control is initialized and has the correct transition state
      const radioGroup = canvas.getByRole("radiogroup")
      expect(radioGroup.getAttribute("data-initialized")).toBe("true")
      const span = radioGroup.querySelector("span")
      expect(span).toHaveStyle("transition-duration: 0.2s")
    })
  },
} satisfies Meta<typeof SegmentedControl>

export default meta

type Story = StoryObj<typeof SegmentedControl>

export const Default: Story = {
  args: {
    data: ["One", "Two", "Three"],
  },
}

export const FullWidth: Story = {
  args: {
    fullWidth: true,
  },
}

export const Disabled: Story = {
  args: {
    disabled: true,
  },
}

export const PartlyDisabled: Story = {
  args: {
    data: [
      { value: "One", label: "One" },
      { value: "Two", label: "Two", disabled: true },
      { value: "Three", label: "Three", disabled: true },
    ],
  },
}

export const Vertical: Story = {
  args: {
    data: ["One", "Two", "Three"],
    orientation: "vertical",
  },
}

export const ExtraSmall: Story = {
  args: {
    data: ["One", "Two", "Three"],
    size: "xs",
  },
}

export const Small: Story = {
  args: {
    data: ["One", "Two", "Three"],
    size: "sm",
  },
}

export const Medium: Story = {
  args: {
    data: ["One", "Two", "Three"],
    size: "md",
  },
}

export const Large: Story = {
  args: {
    data: ["One", "Two", "Three"],
    size: "lg",
  },
}

export const ExtraLarge: Story = {
  args: {
    data: ["One", "Two", "Three"],
    size: "xl",
  },
}
