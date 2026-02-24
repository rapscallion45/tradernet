import { Meta, StoryObj } from "@storybook/react"
import { fn } from "@storybook/test"
import { TextInput } from "@mantine/core"
import { PageHeader } from "./PageHeader"
import { Title } from "../Title/Title"
import { Button } from "../Button/Button"

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
    leftSection: [<TextInput placeholder={"Search"} miw={250} />],
    rightSection: [
      <Button variant={"subtle"} onClick={fn()}>
        Cancel
      </Button>,
      <Button variant={"filled"} disabled onClick={fn()}>
        Create search
      </Button>,
    ],
    secondaryActions: [
      { icon: "trash-can", label: "Delete", onClick: fn() },
      { icon: "info", label: "Information", onClick: fn() },
      { label: "No Icon", onClick: fn() },
    ],
  },
}

export const ReallyLongTitle: Story = {
  args: {
    ...ComplexPage.args,
    title: (
      <Title highlight={"really"}>
        This is a really long title that might wrap onto multiple lines depending on the screen size and it's quite annoying but we need to deal
      </Title>
    ),
    description: "This is a description that goes along with the really long title. It provides additional context and information about the page.",
  },
}
