import { Meta, StoryObj } from "@storybook/react"
import { ActionCard, ActionCardProps } from "./ActionCard"
import { fn } from "@storybook/test"
import { Group, Stack } from "@mantine/core"
import { iconControl } from "../../../storybook/mixins"
import React from "react"
import { CardGrid } from "../CardGrid/CardGrid"

const meta = {
  title: "Tradernet/Cards/ActionCard",
  component: ActionCard,
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
    secondaryActions: {
      control: false,
      description: "The actions that go in the kebab menu",
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
    onClick: fn(),
    secondaryActions: [
      { icon: "pen-to-square", label: "Edit", onClick: fn() },
      { icon: "copy", label: "Clone", onClick: fn() },
      { icon: "trash-can", label: "Delete", onClick: fn() },
    ],
  },
}

// A card that is featured will have a purple background and white text
export const FeaturedCard: Story = {
  args: {
    ...RegularCard.args,
    icon: "plus",
    featured: true,
    text: "Featured Card",
    secondaryActions: undefined,
  },
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

export const Active: Story = {
  args: {
    ...RegularCard.args,
    text: RegularCard.args?.text + " (Active)",
    active: true,
  },
}

export const ModifiedAndActive: Story = {
  args: {
    ...RegularCard.args,
    text: RegularCard.args?.text + " (Both)",
    modified: true,
    active: true,
  },
}

// A card that is disabled will have a gray background and gray text
export const DisabledCard: Story = {
  args: {
    ...RegularCard.args,
    text: "Disabled Search",
    disabled: true,
  },
}

export const WithTooltip: Story = {
  args: {
    ...RegularCard.args,
    onClick: undefined,
    text: "Search with Tooltip",
    icon: "ban",
    tooltip: "You do not have access to this search",
  },
}

// Sometimes there will be no primary action - an app can not be run, but can still be ed for example
export const NoPrimaryAction: Story = {
  args: {
    ...RegularCard.args,
    text: "Editable Search",
    icon: undefined,
    onClick: undefined,
  },
}

// Sometimes there will be no secondary action - an app can not be edited, but can still be run for example
export const NoSecondaryAction: Story = {
  args: {
    ...RegularCard.args,
    text: "Runnable Search",
    secondaryActions: undefined,
  },
}

// Secondary actions should be able to exceed the 200px minimum width
export const LongSecondaryActionText: Story = {
  args: {
    ...RegularCard.args,
    text: "Runnable Search",
    secondaryActions: [
      { icon: "pen-to-square", label: "Edit", onClick: fn() },
      { icon: "copy", label: "Clone this search because you need a new one. Go on, why not?", onClick: fn() },
      { icon: "trash-can", label: "Delete", onClick: fn() },
    ],
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

export const InStack: Story = {
  args: {
    ...RegularCard.args,
  },
  render: (args) => (
    <Stack>
      <ActionCard {...args} />
      <ActionCard {...args} />
      <ActionCard {...args} />
      <ActionCard {...args} />
      <ActionCard {...args} />
    </Stack>
  ),
}

export const InGroup: Story = {
  args: {
    ...RegularCard.args,
  },
  render: (args) => (
    <Group>
      <ActionCard {...args} />
      <ActionCard {...args} />
      <ActionCard {...args} />
      <ActionCard {...args} />
      <ActionCard {...args} />
    </Group>
  ),
}

// A nice way to get a view on how the cards look together
export const GridExample3Items = () => (
  <CardGrid>
    <ActionCard {...(FeaturedCard.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
  </CardGrid>
)

// A nice way to get a view on how the cards look together
export const GridExample6Items = () => (
  <CardGrid>
    <ActionCard {...(FeaturedCard.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
    <ActionCard {...(NoPrimaryAction.args as ActionCardProps)} />
    <ActionCard {...(NoSecondaryAction.args as ActionCardProps)} />
    <ActionCard {...(TruncatedText.args as ActionCardProps)} />
  </CardGrid>
)

// A nice way to get a view on how the cards look together
export const SimpleGridExample3Items = () => (
  <CardGrid>
    <ActionCard {...(FeaturedCard.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
  </CardGrid>
)

// A nice way to get a view on how the cards look together
export const SimpleGridExample6Items = () => (
  <CardGrid>
    <ActionCard {...(FeaturedCard.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
    <ActionCard {...(NoPrimaryAction.args as ActionCardProps)} />
    <ActionCard {...(NoSecondaryAction.args as ActionCardProps)} />
    <ActionCard {...(TruncatedText.args as ActionCardProps)} />
  </CardGrid>
)

// A nice way to get a view on how the cards look together
export const SimpleGridExample9Items = () => (
  <CardGrid>
    <ActionCard {...(FeaturedCard.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
    <ActionCard {...(NoPrimaryAction.args as ActionCardProps)} />
    <ActionCard {...(NoSecondaryAction.args as ActionCardProps)} />
    <ActionCard {...(TruncatedText.args as ActionCardProps)} />
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
    <ActionCard {...(NoPrimaryAction.args as ActionCardProps)} />
  </CardGrid>
)

// A nice way to get a view on how the cards look together
export const ModifiedAndActiveGrid = () => (
  <CardGrid>
    <ActionCard {...(RegularCard.args as ActionCardProps)} />
    <ActionCard {...(DisabledCard.args as ActionCardProps)} />
    <ActionCard {...(ModifiedCard.args as ActionCardProps)} />
    <ActionCard {...(ModifiedCard.args as ActionCardProps)} />
    <ActionCard {...(Active.args as ActionCardProps)} />
    <ActionCard {...(NoPrimaryAction.args as ActionCardProps)} />
  </CardGrid>
)
