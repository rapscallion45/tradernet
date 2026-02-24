import type { Meta, StoryObj } from "@storybook/react"
import { SectionHeading } from "./SectionHeading"
import { textOptions } from "../../../storybook/mixins"

const meta = {
  title: "Tradernet/SectionHeading",
  component: SectionHeading,
  parameters: {
    layout: "centered",
  },
  argTypes: {
    children: {
      options: textOptions,
    },
  },
  args: {
    children: textOptions[0],
  },
} satisfies Meta<typeof SectionHeading>

export default meta
type Story = StoryObj<typeof SectionHeading>

export const Default: Story = {}
