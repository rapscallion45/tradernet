import type { Meta, StoryObj } from "@storybook/react"
import ActionIcon from "./ActionIcon"
import { iconControl, sizeControl, variantControl } from "../../storybook/mixins"

const meta = {
  title: "Tradernet/ActionIcon",
  component: ActionIcon,
  parameters: {
    layout: "centered",
  },
  argTypes: {
    icon: iconControl,
    size: sizeControl,
    variant: variantControl,
  },
} satisfies Meta<typeof ActionIcon>

export default meta
type Story = StoryObj<typeof ActionIcon>

export const Default: Story = {
  args: {
    icon: "lock-keyhole",
  },
}

export const Filled: Story = {
  args: {
    icon: "lock-keyhole",
    variant: "filled",
  },
}

export const FilledDisabled: Story = {
  args: {
    icon: "lock-keyhole",
    variant: "filled",
    disabled: true,
  },
}

export const Outline: Story = {
  args: {
    icon: "lock-keyhole",
    variant: "outline",
  },
}

export const OutlineDisabled: Story = {
  args: {
    icon: "lock-keyhole",
    variant: "outline",
    disabled: true,
  },
}

export const Subtle: Story = {
  args: {
    icon: "lock-keyhole",
    variant: "subtle",
  },
}

export const SubtleDisabled: Story = {
  args: {
    icon: "lock-keyhole",
    variant: "subtle",
    disabled: true,
  },
}

/**
 * Sizes
 */

export const ExtraSmall: Story = {
  args: {
    icon: "lock-keyhole",
    size: "xs",
  },
}

export const Small: Story = {
  args: {
    icon: "lock-keyhole",
    size: "sm",
  },
}

export const Medium: Story = {
  args: {
    icon: "lock-keyhole",
    size: "md",
  },
}

export const Large: Story = {
  args: {
    icon: "lock-keyhole",
    size: "lg",
  },
}

export const ExtraLarge: Story = {
  args: {
    icon: "lock-keyhole",
    size: "xl",
  },
}
