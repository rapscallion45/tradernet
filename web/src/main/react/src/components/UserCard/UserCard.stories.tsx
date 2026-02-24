import { Meta, StoryObj } from "@storybook/react"
import { fn } from "@storybook/test"
import { UserCard, UserCardProps } from "./UserCard"
import drPepper from "./../../images/pink-hair-girl.png"
import { CardGrid } from "../CardGrid/CardGrid"
import React from "react"

const meta = {
  title: "Tradernet/Cards/UserCard",
  component: UserCard,
  argTypes: {
    onClick: {
      control: false,
      description: "The action to run when the card is clicked",
      table: {
        category: "Primary Action",
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
} satisfies Meta<typeof UserCard>

export default meta
type Story = StoryObj<typeof UserCard>

export const Default: Story = {
  args: {
    username: "john.doe",
    fullName: "John Doe",
    onClick: fn(),
    groups: ["administrators", "users", "developers"],
    secondaryActions: [
      { icon: "copy", label: "Clone", onClick: fn() },
      { icon: "trash-can", label: "Delete", onClick: fn() },
    ],
  },
}

export const Admin: Story = {
  args: {
    ...Default.args,
    username: "admin",
    fullName: "Adam Min",
    isAdmin: true,
  },
}

export const Disabled: Story = {
  args: {
    ...Default.args,
    username: "disabled.user",
    fullName: "Dee Sabled",
    disabled: true,
  },
}

export const Modified: Story = {
  args: {
    ...Default.args,
    username: "modified.user",
    fullName: "Mo D. Fied",
    modified: true,
  },
}

export const LongGroupName: Story = {
  args: {
    ...Default.args,
    groups: ["administrators", "a-very-long-group-name-that-is-annoying-to-render", "users"],
  },
}

export const LotsOfGroups: Story = {
  args: {
    ...Default.args,
    groups: [
      "administrators",
      "a-very-long-group-name-that-is-annoying-to-render",
      "users",
      "developers",
      "managers",
      "interns",
      "contractors",
      "guests",
      "support",
      "sales",
    ],
  },
}

export const NoGroups: Story = {
  args: {
    ...Default.args,
    groups: [],
  },
}

export const NoFullName: Story = {
  args: {
    ...Default.args,
    fullName: undefined,
  },
}

export const UsernameTooLong: Story = {
  args: {
    ...Default.args,
    username: "this.is.a.really.long.username.that.should.be.truncated.because.it.probably.wont.fit.on.a.card.of.a.reasonable.size",
  },
}

export const FullNameTooLong: Story = {
  args: {
    ...Default.args,
    fullName: "This is a really long full name that should be truncated because it probably won't fit on a card of a reasonable size",
  },
}

export const WithAvatar: Story = {
  args: {
    ...Default.args,
    username: "dr.pepper",
    fullName: "Cherry Pepper",
    avatarUrl: drPepper,
  },
}

export const UserCardGrid = () => (
  <CardGrid>
    <UserCard {...(Default.args as UserCardProps)} />
    <UserCard {...(Admin.args as UserCardProps)} />
    <UserCard {...(Disabled.args as UserCardProps)} />
    <UserCard {...(Modified.args as UserCardProps)} />
    <UserCard {...(NoFullName.args as UserCardProps)} />
    <UserCard {...(NoGroups.args as UserCardProps)} />
    <UserCard {...(LotsOfGroups.args as UserCardProps)} />
    <UserCard {...(UsernameTooLong.args as UserCardProps)} />
    <UserCard {...(FullNameTooLong.args as UserCardProps)} />
    <UserCard {...(WithAvatar.args as UserCardProps)} />
  </CardGrid>
)
