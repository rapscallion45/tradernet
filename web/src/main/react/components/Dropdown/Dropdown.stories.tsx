import type { Meta, StoryObj } from "@storybook/react"
import { FC, useState } from "react"
import { Group } from "@mantine/core"
import { sizeControl } from "../../storybook/mixins"
import Dropdown, { DropdownProps, Option } from "components/Dropdown/Dropdown"
import Button from "components/Button/Button"

const pageData: Option[] = [
  { value: "1", label: "Page 1" },
  { value: "2", label: "Page 2" },
  { value: "3", label: "Page 3" },
]

const operators: Option[] = [
  { value: "and", label: "And" },
  { value: "or", label: "Or" },
]

const meta = {
  title: "Tradernet/Dropdown",
  component: Dropdown,
  parameters: {
    layout: "centered",
  },
  args: {
    data: pageData,
  },
  argTypes: {
    size: sizeControl,
  },
} satisfies Meta<typeof Dropdown>

export default meta
type Story = StoryObj<typeof Dropdown>

const DropdownWithState: FC<DropdownProps<Option>> = (args) => {
  const [value, setValue] = useState<Option | undefined>(args.value)
  return <Dropdown {...args} value={value} onChange={setValue} />
}

export const Default: Story = {
  render: (args) => <DropdownWithState {...args} />,
  args: {
    data: pageData,
    value: pageData[0],
  },
}

export const DM_Binding: Story = {
  render: (args) => (
    <Group>
      <DropdownWithState {...args} />
      <Button size={"sm"} variant={"outline"} leftIcon={"plus"}>
        Add key
      </Button>
      <Button size={"sm"} variant={"outline"} leftIcon={"object-group"}>
        Add group
      </Button>
    </Group>
  ),
  args: {
    data: operators,
    value: operators[0],
    width: 80,
  },
}
