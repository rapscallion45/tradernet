import { Meta, StoryObj } from "@storybook/react"
import { SectionHeader } from "./SectionHeader"
import { Text, Tabs, Stack, SegmentedControl, ButtonGroup, TextInput } from "@mantine/core"
import { Button } from "../Button/Button"
import { fn } from "@storybook/test"
import { CardGrid } from "../CardGrid/CardGrid"
import { CheckboxCard } from "../CheckboxCard/CheckboxCard"
import { FC } from "react"

type Story = StoryObj<typeof SectionHeader>

const CheckboxCards: FC = () => {
  return (
    <CardGrid>
      <CheckboxCard label={"Mic check"} />
      <CheckboxCard label={"Check one"} />
      <CheckboxCard label={"Check two"} />
      <CheckboxCard label={"Check check"} />
      <CheckboxCard label={"Hello is this thing on?"} />
    </CardGrid>
  )
}

export default {
  title: "Tradernet/SectionHeader",
  component: SectionHeader,
} satisfies Meta<typeof SectionHeader>

export const HeaderWithRightSection: Story = {
  args: {
    description: "Modify the key bindings for this document definition.",
    rightSection: [
      <ButtonGroup>
        <Button size={"sm"} leftIcon={"table-cells"} variant={"outline"} onClick={fn()}>
          Grid
        </Button>
        <Button size={"sm"} leftIcon={"list-ul"} variant={"outline"} onClick={fn()}>
          List
        </Button>
      </ButtonGroup>,
      <Button variant={"filled"} size={"sm"} leftIcon={"plus"} onClick={fn()}>
        New Key Definition
      </Button>,
    ],
  },

  render: (args) => (
    <Tabs defaultValue={"rightSection"}>
      <Tabs.List>
        <Tabs.Tab value={"rightSection"}>Right Section</Tabs.Tab>
      </Tabs.List>
      <Tabs.Panel value="rightSection">
        <Stack gap={"lg"}>
          <SectionHeader {...args} />
          <CheckboxCards />
        </Stack>
      </Tabs.Panel>
    </Tabs>
  ),
}

export const HeaderWithLeftSection: Story = {
  args: {
    leftSection: [
      <TextInput size={"sm"} variant={"filled"} placeholder={"Filter your search..."} />,
      <SegmentedControl
        size={"sm"}
        maw={"500px"}
        value={"StringKeys"}
        data={[
          { label: "String", value: "StringKeys" },
          { label: "Number", value: "NumberKeys" },
          { label: "Date", value: "DateKeys" },
        ]}
        onChange={fn()}
      />,
    ],
  },

  render: (args) => (
    <Tabs defaultValue={"leftSection"}>
      <Tabs.List>
        <Tabs.Tab value={"leftSection"}>Left Section</Tabs.Tab>
      </Tabs.List>
      <Tabs.Panel value={"leftSection"}>
        <Stack>
          <SectionHeader {...args} />
          <CheckboxCards />
        </Stack>
      </Tabs.Panel>
    </Tabs>
  ),
}

export const MultipleTabs: Story = {
  render: () => (
    <Tabs defaultValue={"leftSection"}>
      <Tabs.List>
        <Tabs.Tab value="leftSection">Left Section</Tabs.Tab>
        <Tabs.Tab value="leftWithDesc">Left with desc</Tabs.Tab>
        <Tabs.Tab value="rightSection">Right Section</Tabs.Tab>
      </Tabs.List>
      <Tabs.Panel value={"leftSection"}>
        <Stack gap="lg">
          <SectionHeader
            leftSection={[
              <TextInput size="sm" variant="filled" placeholder="Filter your search..." />,
              <SegmentedControl
                size="sm"
                maw={500}
                value="StringKeys"
                data={[
                  { label: "String", value: "StringKeys" },
                  { label: "Number", value: "NumberKeys" },
                  { label: "Date", value: "DateKeys" },
                ]}
                onChange={fn()}
              />,
            ]}
          />

          <CheckboxCards />
        </Stack>
      </Tabs.Panel>
      <Tabs.Panel value={"leftWithDesc"}>
        <Stack gap="lg">
          <SectionHeader
            description={"This is a description..."}
            leftSection={[
              <SegmentedControl
                size="sm"
                maw={500}
                value="StringKeys"
                data={[
                  { label: "String", value: "StringKeys" },
                  { label: "Number", value: "NumberKeys" },
                  { label: "Date", value: "DateKeys" },
                ]}
                onChange={fn()}
              />,
            ]}
          />

          <CheckboxCards />
        </Stack>
      </Tabs.Panel>
      <Tabs.Panel value={"rightSection"}>
        <Stack gap="lg">
          <SectionHeader
            description={"Modify the key bindings for this document definition."}
            rightSection={[
              <ButtonGroup>
                <Button size="sm" leftIcon="table-cells" variant="outline" onClick={fn()}>
                  Grid
                </Button>
                <Button size="sm" leftIcon="list-ul" variant="outline" onClick={fn()}>
                  List
                </Button>
              </ButtonGroup>,
              <Button variant="filled" size="sm" leftIcon="plus" onClick={fn()}>
                New Key Definition
              </Button>,
            ]}
          />
          <CheckboxCards />
        </Stack>
      </Tabs.Panel>
    </Tabs>
  ),
}
