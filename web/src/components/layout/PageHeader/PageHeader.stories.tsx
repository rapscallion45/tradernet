import { Meta, StoryObj } from "@storybook/react"
import PageHeader from "components/layout/PageHeader/PageHeader"
import Title from "components/Title/Title"
import Button from "components/Button/Button"
import { fn } from "@storybook/test"
import { TextInput } from "@mantine/core"

type Story = StoryObj<typeof PageHeader>

export default {
  title: "Tradernet/PageHeader",
  component: PageHeader,
} satisfies Meta<typeof PageHeader>

export const Welcome: Story = {
  args: {
    title: <Title highlight={"Ben"}>Welcome Ben, what would you like to do today?</Title>,
    description: "To get started use the options below.",
    rightSection: [
      <Button variant={"filled"} leftIcon={"plus"} onClick={fn()}>
        Upload
      </Button>,
    ],
  },
}

export const UploadFiles: Story = {
  args: {
    title: <Title highlight={"Upload"}>Upload files</Title>,
    description: "To get started use the options below.",
  },
}

export const UploadFiles8: Story = {
  args: {
    title: <Title highlight={"Upload"}>Upload files (8)</Title>,
    description: "To get started use the options below.",
    rightSection: [
      <Button variant={"outline"} leftIcon={"plus"} onClick={fn()}>
        Add more
      </Button>,
      <Button variant={"outline"} leftIcon={"trash-can"} onClick={fn()}>
        Delete
      </Button>,
      <Button variant={"filled"} leftIcon={"arrow-up-from-bracket"} onClick={fn()}>
        Upload files (8)
      </Button>,
    ],
  },
}

export const NewSearch: Story = {
  args: {
    title: <Title highlight={"New"}>New search</Title>,
    leftSection: [
      <Button variant={"subtle"} onClick={fn()}>
        Edit name
      </Button>,
    ],
    rightSection: [
      <Button variant={"subtle"} onClick={fn()}>
        Cancel
      </Button>,
      <Button variant={"filled"} disabled onClick={fn()}>
        Create search
      </Button>,
    ],
  },
}

export const ComplexPage: Story = {
  args: {
    title: <Title highlight={1}>Complex Page</Title>,
    leftSection: [<TextInput placeholder={"Search"} />],
    rightSection: [
      <Button variant={"subtle"} onClick={fn()}>
        Cancel
      </Button>,
      <Button variant={"filled"} disabled onClick={fn()}>
        Create search
      </Button>,
    ],
    hiddenSection: [
      <Button variant={"outline"} onClick={fn()} leftIcon={"scalpel"}>
        Advanced Feature
      </Button>,
      <Button variant={"outline"} onClick={fn()}>
        Something rarely used but still important
      </Button>,
    ],
  },
}
