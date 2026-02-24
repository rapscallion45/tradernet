import type { Meta, StoryObj } from "@storybook/react"
import { fn } from "@storybook/test"
import { Button } from "./Button"
import { iconControl, sizeControl, variantControl } from "../../../storybook/mixins"

const meta = {
  title: "Tradernet/Button",
  component: Button,
  parameters: {
    layout: "centered",
  },
  args: {
    size: "md",
    children: "Tradernet Button",
    variant: "outline",
    onClick: fn(),
  },
  argTypes: {
    size: sizeControl,
    variant: variantControl,
    leftIcon: iconControl,
    rightIcon: iconControl,
  },
} satisfies Meta<typeof Button>

export default meta
type Story = StoryObj<typeof Button>

/**
 * Variants
 */
export const Filled: Story = {
  args: {
    variant: "filled",
  },
}

export const FilledDisabled: Story = {
  args: {
    variant: "filled",
    disabled: true,
  },
}

export const FilledDisabledFalse: Story = {
  args: {
    variant: "filled",
    disabled: false,
  },
}

export const FilledLoading: Story = {
  args: {
    variant: "filled",
    loading: true,
  },
}

export const Outline: Story = {
  args: {
    variant: "outline",
  },
}

export const OutlineDisabled: Story = {
  args: {
    variant: "outline",
    disabled: true,
  },
}

export const OutlineLoading: Story = {
  args: {
    variant: "outline",
    loading: true,
  },
}

export const Subtle: Story = {
  args: {
    variant: "subtle",
  },
}
export const SubtleDisabled: Story = {
  args: {
    variant: "subtle",
    disabled: true,
  },
}

export const SubtleLoading: Story = {
  args: {
    variant: "subtle",
    loading: true,
  },
}

/**
 *  Sizes
 */
export const ExtraSmall: Story = {
  args: {
    size: "xs",
    leftIcon: "xmark",
    rightIcon: "xmark",
  },
}

export const Small: Story = {
  args: {
    size: "sm",
    leftIcon: "xmark",
    rightIcon: "xmark",
  },
}

export const Medium: Story = {
  args: {
    size: "md",
    leftIcon: "xmark",
    rightIcon: "xmark",
  },
}

export const Large: Story = {
  args: {
    size: "lg",
    leftIcon: "xmark",
    rightIcon: "xmark",
  },
}

export const ExtraLarge: Story = {
  args: {
    size: "xl",
    leftIcon: "xmark",
    rightIcon: "xmark",
  },
}

/**
 * Icons
 */
export const LeftIcon: Story = {
  args: {
    leftIcon: "xmark",
  },
}

export const RightIcon: Story = {
  args: {
    rightIcon: "xmark",
  },
}
