import type { Meta, StoryObj } from "@storybook/react"
import { FC, useState } from "react"
import Button from "../Button/Button"
import ActionIcon from "components/ActionIcon/ActionIcon"
import { Group, MultiSelect, MultiSelectProps, Stack } from "@mantine/core"
import { sizeControl } from "../../storybook/mixins"
import { AppSize } from "global/types"

const groupedData = [
  { group: "Group 1", items: ["Option 1", "Option 2", "Option 3"] },
  { group: "Group 2", items: ["Option 4", "Option 5", "Option 6"] },
]

const meta = {
  title: "Mantine/MultiSelect",
  component: MultiSelect,
  parameters: {
    layout: "centered",
  },
  args: {
    data: groupedData,
  },
  argTypes: {
    size: sizeControl,
  },
  decorators: [
    (Story) => (
      <Stack w={300}>
        <Story />
      </Stack>
    ),
  ],
} satisfies Meta<typeof MultiSelect>

export default meta
type Story = StoryObj<typeof MultiSelect>

const SelectWithState: FC<MultiSelectProps> = (args) => {
  const [value, setValue] = useState<string[]>([])
  return <MultiSelect {...args} value={value} onChange={setValue} />
}

export const Default: Story = {
  render: (args) => <SelectWithState {...args} />,
  args: {
    data: groupedData,
  },
}

export const Clearable: Story = {
  render: (args) => <SelectWithState {...args} />,
  args: {
    data: groupedData,
    rightSection: null,
    clearable: true,
  },
}

const SelectAndButtons: FC<MultiSelectProps> = (args) => {
  return (
    <Group>
      <SelectWithState w={200} {...args} />
      <Button variant={"outline"} size={args.size as AppSize}>
        Button of same size
      </Button>
      <ActionIcon matchFormSize icon={"stars"} size={args.size as AppSize} />
    </Group>
  )
}

export const AlongsideButton: Story = {
  render: (args) => <SelectAndButtons {...args} />,
  args: {
    size: "xs",
  },
}

export const GroupedOptdions: Story = {
  render: (args) => <SelectWithState {...args} />,
  args: {
    data: groupedData,
    value: [],
  },
}
