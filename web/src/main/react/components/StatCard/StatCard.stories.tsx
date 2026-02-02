import type { Meta, StoryObj } from "@storybook/react"
import { StatCard } from "./StatCard"
import { expect, fn, userEvent, within } from "@storybook/test"
import { iconControl } from "../../storybook/mixins"

const meta = {
  title: "Tradernet/StatCard",
  component: StatCard,
  parameters: {
    layout: "centered",
  },
  argTypes: {
    text: {
      control: "text",
      description: "The main title text",
      table: {
        category: "Primary Action",
      },
    },
    onClick: {
      control: false,
      description: "The action to run when the card is clicked",
      table: {
        category: "Primary Action",
      },
    },
    icon: {
      description: "The icon to display next to the title",
      table: {
        ...iconControl.table,
        category: "Primary Action",
      },
    },
    secondaryText: {
      control: "text",
      description: "The secondary text, shows on hover",
      table: {
        category: "Secondary Action",
      },
    },
  },
} satisfies Meta<typeof StatCard>

export default meta

type Story = StoryObj<typeof StatCard>

export const TicketsOpen: Story = {
  args: {
    text: "000",
    icon: "arrow-right",
    secondaryText: "Tickets open",
    onClick: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    const card = canvas.getByLabelText("stat-card")
    await userEvent.click(card)
    expect(args.onClick).toHaveBeenCalled()
  },
}

export const TicketsAwaitingFeedback: Story = {
  args: {
    text: "000",
    icon: "arrow-right",
    secondaryText: "Tickets awaiting feedback",
    onClick: fn(),
  },
}

export const NoAction: Story = {
  args: {
    text: "69%",
    secondaryText: "Tickets answered within 1 hour",
  },
}

export const Narrow: Story = {
  args: {
    text: "99%",
    secondaryText: "People that prefer wide cards",
    narrow: true,
  },
}
