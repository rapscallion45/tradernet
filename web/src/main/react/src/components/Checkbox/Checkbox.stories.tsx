import type { Meta, StoryObj } from "@storybook/react"
import { Checkbox, Flex } from "@mantine/core"

/**
 * Checkbox is showcased here with some simple examples.
 *
 * We do not have our own Checkbox component as it is not needed. Simply using the Mantine checkbox will use the
 * correct styles, as set in the theme.
 */
const meta = {
  component: Checkbox,
  title: "Mantine/Checkbox",
  parameters: {
    layout: "padded",
  },
  argTypes: {
    checked: { control: "boolean" },
    indeterminate: { control: "boolean" },
    disabled: { control: "boolean" },
    label: { control: "text" },
    labelPosition: { control: "inline-radio", options: ["right", "left"] },
    description: { control: "text" },
    error: { control: "text" },
    size: { control: "inline-radio", options: ["xs", "sm", "md", "lg", "xl"] },
  },
  decorators: [
    (Story) => (
      <Flex m={"xs"}>
        <Story />
      </Flex>
    ),
  ],
} satisfies Meta<typeof Checkbox>

export default meta

type Story = StoryObj<typeof meta>

/**
 *  Default story
 */
export const Default: Story = {}

/**
 *  Checked story
 */
export const Checked: Story = {
  args: {
    checked: true,
  },
}

/**
 *  Indeterminate story
 */
export const Indeterminate: Story = {
  args: {
    indeterminate: true,
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    disabled: true,
  },
}

/**
 *  With Label story
 */
export const WithLabel: Story = {
  args: {
    label: "Tradernet Checkbox",
  },
}

/**
 *  With Label Left story
 */
export const WithLabelLeft: Story = {
  args: {
    label: "Tradernet Checkbox",
    labelPosition: "left",
  },
}

/**
 *  With Description story
 */
export const WithDescription: Story = {
  args: {
    label: "Tradernet Checkbox",
    description: "Tradernet Checkbox description example text",
  },
}

/**
 *  With Error story
 */
export const WithError: Story = {
  args: {
    label: "Tradernet Checkbox",
    error: "There is something wrong!",
  },
}

/**
 *  Various sizes
 */
export const ExtraSmall = {
  args: {
    label: "Tradernet Extra Small Checkbox",
    description: "Tradernet Checkbox description example text",
    size: "xs",
  },
}

export const Small = {
  args: {
    label: "Tradernet Small Checkbox",
    description: "Tradernet Checkbox description example text",
    size: "sm",
  },
}

export const Medium = {
  args: {
    label: "Tradernet Medium Checkbox",
    description: "Tradernet Checkbox description example text",
    size: "md",
  },
}

export const Large = {
  args: {
    label: "Tradernet Large Checkbox",
    description: "Tradernet Checkbox description example text",
    size: "lg",
  },
}

export const ExtraLarge = {
  args: {
    label: "Tradernet Extra Large Checkbox",
    description: "Tradernet Checkbox description example text",
    size: "xl",
  },
}
