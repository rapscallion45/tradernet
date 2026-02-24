import { fn } from "@storybook/test"
import { InfoCard, InfoCardProps } from "./InfoCard"
import { Meta, StoryObj } from "@storybook/react"
import { iconControl } from "../../../storybook/mixins"
import { CardGrid } from "../CardGrid/CardGrid"

/**
 * InfoCard is showcased here with some simple examples.
 */
const meta = {
  component: InfoCard,
  title: "Tradernet/Cards/InfoCard",
  argTypes: {
    text: {
      control: "text",
      description: "The main title text",
    },
    secondaryText: {
      control: "text",
      description: "The secondary text, shows under the title",
    },
    onClick: {
      control: false,
      description: "The action to run when the card is clicked",
    },
    icon: {
      description: "The icon to display next to the title",
      table: { ...iconControl.table },
    },
  },
} satisfies Meta<typeof InfoCard>

export default meta

type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    text: "INFO CARD",
    secondaryText: "This is some additional information",
    icon: "circle-info",
    onClick: fn(),
  },
}

export const WithNoAction: Story = {
  args: {
    ...Default.args,
    onClick: undefined,
  },
}

export const WithLongText: Story = {
  args: {
    ...Default.args,
    text: "This is a very long title that should be truncated with an ellipsis if it exceeds the width of the card",
    secondaryText: "This is some additional information that is also quite long and should be truncated with an ellipsis if it exceeds the width of the card",
  },
}

export const WithCustomIcon: Story = {
  args: {
    ...Default.args,
    icon: "bell",
  },
}

export const WithCustomColor: Story = {
  args: {
    ...Default.args,
    color: "green",
  },
}

export const WithTooltip: Story = {
  args: {
    ...Default.args,
    tooltip: "This is a tooltip that provides more information about the card. It has a maximum width and can break onto multiple lines.",
  },
}

export const InfoCardGrid = () => (
  <CardGrid>
    <InfoCard {...(Default.args as InfoCardProps)} />
    <InfoCard {...(WithNoAction.args as InfoCardProps)} />
    <InfoCard {...(WithLongText.args as InfoCardProps)} />
    <InfoCard {...(WithCustomIcon.args as InfoCardProps)} />
    <InfoCard {...(WithCustomColor.args as InfoCardProps)} />
  </CardGrid>
)
