import { Meta, StoryObj } from "@storybook/react"
import { Highlight } from "@mantine/core"
import { colorControl } from "../../../storybook/mixins"

/**
 * Highlight is showcased here with some simple examples.
 */
const meta = {
  component: Highlight,
  title: "Mantine/Highlight",
  argTypes: {
    color: colorControl,
  },
} satisfies Meta<typeof Highlight>

export default meta

type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    highlight: "highlight",
    children: "This is a simple highlight example to highlight how it works.",
  },
}
