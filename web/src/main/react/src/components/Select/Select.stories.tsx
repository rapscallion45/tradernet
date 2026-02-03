import type { Meta, StoryObj } from "@storybook/react"
import { FC, useState } from "react"
import Button from "../Button/Button"
import ActionIcon from "components/ActionIcon/ActionIcon"
import { Group, Select, SelectProps, Stack } from "@mantine/core"
import { sizeControl } from "../../storybook/mixins"
import { AppSize } from "global/types"

const pageData = ["Page 1", "Page 2", "Page 3"]

const groupedData = [
  { group: "Group 1", items: ["Option 1", "Option 2", "Option 3"] },
  { group: "Group 2", items: ["Option 4", "Option 5", "Option 6"] },
]

const meta = {
  title: "Mantine/Select",
  component: Select,
  parameters: {
    layout: "centered",
  },
  args: {
    data: pageData,
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
} satisfies Meta<typeof Select>

export default meta
type Story = StoryObj<typeof Select>

const SelectWithState: FC<SelectProps> = (args) => {
  const [value, setValue] = useState<string | null>(pageData[0])
  return <Select {...args} value={value} onChange={setValue} />
}

export const Default: Story = {
  render: (args) => <SelectWithState {...args} />,
  args: {
    data: pageData,
  },
}

export const Clearable: Story = {
  render: (args) => <SelectWithState {...args} />,
  args: {
    data: pageData,
    value: "Page 1",
    rightSection: null,
    clearable: true,
  },
}

const SelectAndButtons: FC<SelectProps> = (args) => {
  return (
    <Group>
      <SelectWithState {...args} w={200} />
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

export const GroupedOptions: Story = {
  render: (args) => <SelectWithState {...args} />,
  args: {
    data: groupedData,
    value: "Option 1",
  },
}
