import { BaseCard } from "./BaseCard"
import { Meta, StoryObj } from "@storybook/react"
import { fn } from "@storybook/test"
import squareCardClasses from "./SquareCard.module.css"
import flexBasisClasses from "./FlexBasisCard.module.css"
import { Group, SimpleGrid, Stack, Text, Title } from "@mantine/core"
import { textOptions } from "../../../storybook/mixins"
import { Icon } from "../Icon/Icon"
import React from "react"

const meta = {
  title: "Tradernet/Cards/BaseCard",
  component: BaseCard,
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
    active: {
      control: "boolean",
      description: "Whether the card represents a thing in an active state",
      table: {
        category: "Toggles",
      },
    },
  },
  args: {
    onClick: fn(),
    secondaryActions: [
      { icon: "pen-to-square", label: "Edit", onClick: fn() },
      { icon: "copy", label: "Clone", onClick: fn() },
      { icon: "trash-can", label: "Delete", onClick: fn() },
    ],
  },
} satisfies Meta<typeof BaseCard>

export default meta
type Story = StoryObj<typeof BaseCard>

export const Default: Story = {
  args: {
    children: "The base card starts with width at 100%, which we can override in child cards.",
  },
}

export const NotClickable: Story = {
  args: {
    children: "Even if it has no main action, it can still have secondary ones.",
    onClick: undefined,
  },
}

export const Featured: Story = {
  args: {
    children: "... unless it's a featured card, as these are just for calls-to-action.",
    featured: true,
  },
}

export const Disabled: Story = {
  args: {
    children: "Disabled cards can't have any actions associated.",
    disabled: true,
  },
}

export const Modified: Story = {
  args: {
    children: "When something is being / has been edited, the modified prop shows a nice glow effect.",
    modified: true,
  },
}

export const Active: Story = {
  args: {
    children: "The active prop shows a different border to indicate selection.",
    active: true,
  },
}

export const ModifiedAndActive: Story = {
  args: {
    children: "The modified and active props can be combined, so active should take precedence.",
    modified: true,
    active: true,
  },
}

export const Square: Story = {
  args: {
    children: "This card has a fixed width and height, because we passed in a custom CSS module, which is merged with the base one.",
    classes: squareCardClasses,
  },
}

export const FlexBasis1: Story = {
  args: {
    children: "The card will flex between 5 and 15 rem.",
    classes: flexBasisClasses,
  },
  render: (args) => (
    <Group w={"400px"} grow bd={"1px solid red"} p={"md"}>
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
    </Group>
  ),
}

export const FlexBasis2: Story = {
  args: {
    children: <div>The card will flex between 5 and 15 rem.</div>,
    classes: flexBasisClasses,
  },
  render: (args) => (
    <Group w={"500px"} grow bd={"1px solid red"} p={"md"}>
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
    </Group>
  ),
}

export const FlexBasis3: Story = {
  args: {
    children: <div>The card will flex between 5 and 15 rem.</div>,
    classes: flexBasisClasses,
  },
  render: (args) => (
    <Group w={"600px"} grow bd={"1px solid red"} p={"md"}>
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
    </Group>
  ),
}

export const FlexBasis4: Story = {
  args: {
    children: <div>The card will flex between 5 and 15 rem.</div>,
    classes: flexBasisClasses,
  },
  render: (args) => (
    <Group w={"100%"} grow bd={"1px solid red"} p={"md"}>
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
    </Group>
  ),
}

export const FlexBasisSimpleGrid: Story = {
  args: {
    children: <div>The card will flex between 5 and 15 rem.</div>,
    classes: flexBasisClasses,
  },
  render: (args) => (
    <SimpleGrid cols={4}>
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
      <BaseCard {...args} />
    </SimpleGrid>
  ),
}

export const SmallPadding: Story = {
  args: {
    children: "This card has small padding.",
    onClick: undefined,
    p: "sm",
  },
}

export const ExtraLargePadding: Story = {
  args: {
    children: "This card has extra large padding.",
    onClick: undefined,
    p: "xxl",
  },
}

export const CustomHeight: Story = {
  args: {
    onClick: undefined,
    h: "10rem",
    children: (
      <Stack h={"100%"} justify={"center"}>
        {textOptions[1]}
      </Stack>
    ),
  },
}

export const CustomWidth: Story = {
  args: {
    onClick: undefined,
    w: "40rem",
    children: (
      <Group w={"100%"} justify={"flex-end"}>
        {textOptions[1]}
      </Group>
    ),
  },
}

// An example of how we could use the card to build more complex components
export const NoteCard: Story = {
  args: {
    onClick: undefined,
    p: "lg",
    w: "50rem",
    children: (
      <Stack h="100%" justify={"space-between"}>
        <Group justify={"space-between"}>
          <Group>
            <Icon name={"user"} />
            <Title order={4}>A. NAME</Title>
          </Group>
          <Group>
            <Text>3:44 PM</Text>
            <Icon name={"ellipsis-vertical"} />
          </Group>
        </Group>
        <Text>{textOptions[2]}</Text>
      </Stack>
    ),
  },
}
