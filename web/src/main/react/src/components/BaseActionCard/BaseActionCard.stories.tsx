import { Meta, StoryObj } from "@storybook/react"
import { BaseActionCard } from "./BaseActionCard"
import { fn, within } from "@storybook/test"
import { Title, Tooltip } from "@mantine/core"

const meta = {
  title: "Tradernet/Cards/BaseActionCard",
  component: BaseActionCard,
  argTypes: {
    onClick: {
      control: false,
      description: "The action to run when the card is clicked",
      table: {
        category: "Primary Action",
      },
    },
    featured: {
      control: "boolean",
      description: "Whether the card is a call to action",
      table: {
        category: "Toggles",
      },
    },
    disabled: {
      control: "boolean",
      description: "Whether the card is disabled",
      table: {
        category: "Toggles",
      },
    },
    modified: {
      control: "boolean",
      description: "Whether the card represents a thing in a modified state",
      table: {
        category: "Toggles",
      },
    },
  },
} satisfies Meta<typeof BaseActionCard>

export default meta
type Story = StoryObj<typeof BaseActionCard>

export const TwoTextSections: Story = {
  args: {
    topSection: <div>Top Section</div>,
    bottomSection: <div>Bottom Section</div>,
    onClick: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
  },
}

export const MultipleLines: Story = {
  args: {
    topSection: <div>Top Section</div>,
    bottomSection: <div>Bottom Section is really long and goes onto two lines</div>,
    onClick: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
  },
}

export const LargeTopText: Story = {
  args: {
    topSection: <Title>Top Section</Title>,
    bottomSection: <div>Bottom Section</div>,
    onClick: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
  },
}

export const LargeBottomText: Story = {
  args: {
    topSection: <div>Top Section</div>,
    bottomSection: <Title>Bottom Section</Title>,
    onClick: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
  },
}

// Don't do this! This is just here to check behaviour isn't mental
export const LargeTopAndBottomText: Story = {
  args: {
    topSection: <Title>Top Section</Title>,
    bottomSection: <Title>Bottom Section</Title>,
    onClick: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
  },
}

// A card that is featured will have a purple background and white text
export const FeaturedCard: Story = {
  args: {
    ...TwoTextSections.args,
    featured: true,
  },
}

// a card that is modified will have a purple glow around it
export const ModifiedCard: Story = {
  args: {
    ...TwoTextSections.args,
    modified: true,
  },
}

// A card that is disabled will have a gray background and gray text
export const DisabledCard: Story = {
  args: {
    ...TwoTextSections.args,
    disabled: true,
  },
}

// A very minimal example with just a text
export const ClickableWithTooltip: Story = {
  args: {
    ...TwoTextSections.args,
  },
  render: (args) => (
    <Tooltip label={"Here is a tooltip with some more information"}>
      <BaseActionCard {...args} />
    </Tooltip>
  ),
}

// A very minimal example with just a text
export const NotClickableWithTooltip: Story = {
  args: {
    ...TwoTextSections.args,
    onClick: undefined,
  },
  render: (args) => (
    <Tooltip label={"Here is a tooltip with some more information"}>
      <BaseActionCard {...args} />
    </Tooltip>
  ),
}
