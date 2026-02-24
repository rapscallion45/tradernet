import type { Meta, StoryObj } from "@storybook/react"
import { Center, Group, Stack } from "@mantine/core"
import { fn } from "@storybook/test"
import { FC, useState } from "react"
import { CheckboxCard, CheckboxCardProps } from "./CheckboxCard"
import { CheckboxCardGroup } from "../CheckboxCardGroup/CheckboxCardGroup"

/**
 * CheckboxCard is showcased here with some simple examples.
 */
const meta = {
  component: CheckboxCard,
  title: "Tradernet/CheckboxCard",
  args: {
    checked: false,
    label: "Tradernet Checkbox Card",
    onChange: fn(),
  },
  argTypes: {
    checked: { control: "boolean" },
    modified: { control: "boolean" },
    label: { control: "text" },
    description: { control: "text" },
    disabled: { control: "boolean" },
  },
  render: (args) => <CheckboxCardWithState {...args} />,
} satisfies Meta<typeof CheckboxCard>

export default meta

type Story = StoryObj<typeof meta>

const CheckboxCardWithState: FC<CheckboxCardProps> = (args) => {
  const [checked, setChecked] = useState(args.checked)
  return (
    <Stack>
      <Group>
        <CheckboxCard
          {...args}
          checked={checked}
          externalChecked={args.externalChecked}
          onChange={(newChecked) => {
            console.debug("Checking checked to", newChecked)
            setChecked(newChecked)
          }}
          modified={args.modified ?? checked !== args.checked}
        />
      </Group>
      <p>Checked? {checked ? "Yes" : "No"}</p>
      <p>ExternalChecked? {args.externalChecked ? "Yes" : "No"}</p>
    </Stack>
  )
}

export const Default: Story = {}

export const Checked: Story = {
  args: {
    checked: true,
  },
}

export const Unchecked: Story = {
  args: {
    checked: false,
  },
}

export const DisabledTrue: Story = {
  args: {
    disabled: true,
  },
}

export const DisabledUndefined: Story = {
  args: {
    disabled: undefined,
  },
}

export const DisabledFalse: Story = {
  args: {
    disabled: false,
  },
}

export const WithDescription: Story = {
  args: {
    label: "Tradernet Checkbox Card",
    description: "Tradernet Checkbox Card description example text",
  },
}

export const WithLongDescription: Story = {
  args: {
    label: "Tradernet Checkbox Card",
    description: "Tradernet Checkbox Card description example text and some more text to make it longer",
  },
}

export const ModifiedTrue: Story = {
  args: {
    checked: true,
    label: "Document Search",
    description: "This shows how a card can look when it's been modified, but not yet saved.",
    modified: true,
  },
}

export const ModifiedUndefined: Story = {
  args: {
    checked: true,
    label: "Document Search",
    description: "This shows how a card can look when it's been modified, but not yet saved.",
    modified: undefined,
  },
}

export const ModifiedFalse: Story = {
  args: {
    checked: true,
    label: "Document Search",
    description: "This shows how a card can look when it's been modified, but not yet saved.",
    modified: false,
  },
}

export const ExternalChecked: Story = {
  args: {
    checked: false,
    externalChecked: true,
    label: "Tradernet Checkbox Card",
    description: "Tradernet Checkbox Card description example text",
  },
}

export const ExternalCheckedWithCustomIcon: Story = {
  args: {
    checked: false,
    externalChecked: true,
    externalCheckedIcon: "users",
    label: "Tradernet Checkbox Card",
    description: "Tradernet Checkbox Card description example text",
  },
}

export const InsideGroup = () => (
  <Stack>
    <Center>Group - heights do not match (should use CheckboxCardGroup) ❌</Center>
    <Group>
      <CheckboxCard label="Tradernet Checkbox Card 1" description="Tradernet Checkbox Card description example text" />
      <CheckboxCard
        label="Tradernet Checkbox Card 2"
        description="Tradernet Checkbox Card description example text but this one is a lot longer so it wraps onto two lines"
      />
      <CheckboxCard label="Tradernet Checkbox Card 3" description="Tradernet Checkbox Card description example text" />
      <CheckboxCard label="Tradernet Checkbox Card 4" description="Tradernet Checkbox Card description example text" />
    </Group>
  </Stack>
)

export const InsideStack = () => (
  <Stack>
    <Center>Stack - Cards reach their maximum width at a certain breakpoint (only use inside drawers, modals etc) ❌</Center>
    <CheckboxCard label="Tradernet Checkbox Card 1" description="Tradernet Checkbox Card description example text" />
    <CheckboxCard
      label="Tradernet Checkbox Card 2"
      description="Tradernet Checkbox Card description example text but this one is a lot longer so it wraps onto two lines"
    />
    <CheckboxCard label="Tradernet Checkbox Card 3" description="Tradernet Checkbox Card description example text" />
    <CheckboxCard label="Tradernet Checkbox Card 4" description="Tradernet Checkbox Card description example text" />
  </Stack>
)

export const InsideCardGroup = () => (
  <Stack>
    <Center>CheckboxCardGroup - Using this ensures height is uniform across rows, with multiple cards on a single row ✔️</Center>
    <CheckboxCardGroup>
      <CheckboxCard label="Tradernet Checkbox Card 1" description="Tradernet Checkbox Card description example text" />
      <CheckboxCard
        label="Tradernet Checkbox Card 2"
        description="Tradernet Checkbox Card description example text but this one is a lot longer so it wraps onto two lines"
      />
      <CheckboxCard label="Tradernet Checkbox Card 3" description="Tradernet Checkbox Card description example text" />
      <CheckboxCard label="Tradernet Checkbox Card 4" description="Tradernet Checkbox Card description example text" />
    </CheckboxCardGroup>
  </Stack>
)
