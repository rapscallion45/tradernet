import { Meta, StoryObj } from "@storybook/react"
import { ActionCard, ActionCardProps } from "components/ActionCard/ActionCard"
import { expect, fn, userEvent, within } from "@storybook/test"
import { Group } from "@mantine/core"
import { iconControl } from "../../storybook/mixins"

const meta = {
  title: "Tradernet/ActionCard",
  component: ActionCard,
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
    action: {
      control: false,
      description: "The action to run when the card is clicked",
      table: {
        category: "Primary Action",
      },
    },
    tooltip: {
      control: "text",
      description: "The tooltip for the primary action",
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
    secondaryIcon: {
      description: "The icon to display next to the secondary text, shows on hover",
      table: {
        ...iconControl.table,
        category: "Secondary Action",
      },
    },
    secondaryToolTip: {
      control: "text",
      description: "The tooltip for the secondary action",
      table: {
        category: "Secondary Action",
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
} satisfies Meta<typeof ActionCard>

export default meta
type Story = StoryObj<typeof ActionCard>

export const RegularCard: Story = {
  args: {
    text: "Invoice Search",
    icon: "arrow-right",
    secondaryText: "More Info",
    secondaryIcon: "pen-to-square",
    secondaryToolTip: "Edit this search",
    action: fn(),
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    const card = canvas.getByTestId(`ActionCard-${args.text}`)

    console.log("card", card)

    expect(card).toBeTruthy()

    // check the title exists
    const text = canvas.getByText(args.text)
    expect(text).toBeVisible()

    // click the text
    await userEvent.click(text)
    // check the action was called
    expect(args.action).toHaveBeenCalled()
    // check the secondary action icon exists
    const secondaryIcon = canvas.getByTestId("icon-pen-to-square")
    expect(secondaryIcon).toBeInTheDocument()
    await userEvent.click(secondaryIcon)
    // check the primary action was not called again
    expect(args.action).toHaveBeenCalledTimes(1)
  },
}

// A card that is featured will have a purple background and white text
export const FeaturedCard: Story = {
  args: {
    ...RegularCard.args,
    icon: "plus",
    featured: true,
    text: RegularCard.args?.text + " (Featured)",
  },
  play: RegularCard.play, // just run the same tests to ensure nothing crazy happens when things are purple
}

// a card that is modified will have a purple glow around it
export const ModifiedCard: Story = {
  args: {
    ...RegularCard.args,
    text: RegularCard.args?.text + " (Modified)",
    modified: true,
  },
  play: RegularCard.play, // just run the same tests to ensure nothing crazy happens when things are purple
}

// A card that is disabled will have a gray background and gray text
export const DisabledCard: Story = {
  args: {
    ...RegularCard.args,
    text: "Disabled Search",
    disabled: true,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // check the title exists
    const text = canvas.getByText("Disabled Search")
    expect(text).toBeInTheDocument()
    // click it
    await userEvent.click(text)
    // check both actions were NOT called
    expect(args.action).not.toHaveBeenCalled()
    // check the secondary action icon does not exist
    const secondaryIcon = await canvas.queryByTestId("icon-pen-to-square")
    expect(secondaryIcon).not.toBeInTheDocument()
  },
}

// Sometimes there will be no primary action - an app can not be run, but can still be edited for example
export const NoPrimaryAction: Story = {
  args: {
    ...RegularCard.args,
    text: "Editable Search",
    icon: undefined,
    action: undefined,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // check the title exists
    const text = canvas.getByText("Editable Search")
    expect(text).toBeInTheDocument()
    // click it
    await userEvent.click(text)
    // check the main action is undefined
    expect(args.action).toBe(undefined)
    // check the secondary action icon exists
    const secondaryIcon = await canvas.getByTestId("icon-pen-to-square")
    expect(secondaryIcon).toBeInTheDocument()
    await userEvent.click(secondaryIcon)
    // check the secondary action was called
  },
}

// Sometimes there will be no secondary action - an app can not be edited, but can still be run for example
export const NoSecondaryAction: Story = {
  args: {
    ...RegularCard.args,
    text: "Runnable Search",
    secondaryIcon: undefined,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    // check the title exists
    const text = canvas.getByText("Runnable Search")
    expect(text).toBeInTheDocument()
    // click it
    await userEvent.click(text)
    // check the action was called
    expect(args.action).toHaveBeenCalled()
    // check the secondary action icon does not exist
    const secondaryIcon = canvas.queryByTestId("icon-pen-to-square")
    expect(secondaryIcon).not.toBeInTheDocument()
  },
}

// A title that breaks onto two lines
export const LongText: Story = {
  args: {
    ...RegularCard.args,
    text: "Runnable Search, not editable",
  },
}

// A card with a title that is too long to fit on the card, so we truncate after the second line
export const TruncatedText: Story = {
  args: {
    ...RegularCard.args,
    text: "Runnable Search, not editable, and is too big to fit on the card",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    // check the title exists
    const text = canvas.getByText("Runnable Search, not editable, and is too big to fit on the card")
    expect(text).toBeVisible()
  },
}

// A title that breaks onto two lines
export const LongSecondaryText: Story = {
  args: {
    ...TruncatedText.args,
    secondaryText: "You can click this card to perform a search, or click the icon to edit the search",
  },
}

// A very minimal example with just a text
export const MinimalContent: Story = {
  args: {
    text: "Create new search",
    icon: "arrow-right",
  },
}

// A nice way to get a view on how the cards look together
const GridExample = () => (
  <Group gap={"lg"} justify={"left"}>
    <ActionCard {...(FeaturedCard.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
    <ActionCard {...(NoPrimaryAction.args as ActionCardProps)} />
    <ActionCard {...(NoSecondaryAction.args as ActionCardProps)} />
    <ActionCard {...(TruncatedText.args as ActionCardProps)} />
  </Group>
)

export const CardGrid = () => <GridExample />
