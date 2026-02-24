import type { Meta, StoryObj } from "@storybook/react"
import { ConfirmationModal } from "./ConfirmationModal"
import { expect, fn, screen, userEvent, waitForElementToBeRemoved, within } from "@storybook/test"
import { MultiSelect, Stack, TextInput } from "@mantine/core"
import { useState } from "react"
import { Button } from "../Button/Button"
import { useDisclosure } from "@mantine/hooks"

const meta = {
  component: ConfirmationModal,
  title: "Tradernet/Modals/ConfirmationModal",
  args: {
    title: "Confirmation Modal",
    opened: true,
    onCancel: fn(),
    onConfirm: fn(),
  },
  argTypes: {
    title: { control: "text" },
    opened: { control: "boolean" },
  },
  render: (args) => {
    const [open, setOpen] = useState(false)
    return (
      <>
        <Button onClick={() => setOpen(true)} variant="outline">
          Confirmation Modal
        </Button>
        <ConfirmationModal
          {...args}
          opened={open}
          onCancel={() => {
            args.onCancel()
            setOpen(false)
          }}
          onConfirm={() => {
            args.onConfirm()
            setOpen(false)
          }}
        />
      </>
    )
  },
} satisfies Meta<typeof ConfirmationModal>

export default meta

type Story = StoryObj<typeof ConfirmationModal>

const MockForm = () => {
  return (
    <Stack>
      <TextInput variant={"outline"} label="Name" description={"The name of the thing"} />
      <TextInput variant={"outline"} label="Label" description={"The label of the thing"} />
      <MultiSelect
        variant={"outline"}
        label="Definition"
        description={"The definition related to the thing"}
        data={["Option 1", "Option 2", "Option 3"]}
        clearable
      />
    </Stack>
  )
}

export const Default: Story = {}

export const CanCancel: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    const user = userEvent.setup()

    // open
    const openButton = await canvas.findByRole("button", { name: "Confirmation Modal" })
    await user.click(openButton)

    // use screen as we are querying outside the canvas (portal)
    const dialog = await screen.findByRole("dialog", { name: "Confirmation Modal" })
    expect(dialog).toBeInTheDocument()

    // cancel
    const cancel = await screen.findByRole("button", { name: "Cancel" })
    await user.click(cancel)

    // wait for it to unmount / finish exit animation
    await waitForElementToBeRemoved(dialog)

    // only onCancel should be called when cancelling
    expect(args.onConfirm).not.toHaveBeenCalled()
    expect(args.onCancel).toHaveBeenCalled()
  },
}

export const CanClose: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    const user = userEvent.setup()

    // open
    const openButton = await canvas.findByRole("button", { name: "Confirmation Modal" })
    await user.click(openButton)

    // use screen as we are querying outside the canvas (portal)
    const dialog = await screen.findByRole("dialog", { name: "Confirmation Modal" })
    expect(dialog).toBeInTheDocument()

    // close
    const close = await screen.findByRole("button", { name: /close modal/i })
    await user.click(close)

    // wait for it to unmount / finish exit animation
    await waitForElementToBeRemoved(dialog)

    // only onCancel should be called when closing
    expect(args.onConfirm).not.toHaveBeenCalled()
    expect(args.onCancel).toHaveBeenCalled()
  },
}

export const CanConfirm: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    const user = userEvent.setup()

    // open
    const openButton = await canvas.findByRole("button", { name: "Confirmation Modal" })
    await user.click(openButton)

    // use screen as we are querying outside the canvas (portal)
    const dialog = await screen.findByRole("dialog", { name: "Confirmation Modal" })
    expect(dialog).toBeInTheDocument()

    // confirm
    const confirm = await screen.findByRole("button", { name: "Confirm" })
    await user.click(confirm)

    // wait for it to unmount / finish exit animation
    await waitForElementToBeRemoved(dialog)

    // only onConfirm should be called when confirming
    expect(args.onConfirm).toHaveBeenCalled()
    expect(args.onCancel).not.toHaveBeenCalled()
  },
}

export const CanPressEscapeToClose: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    const user = userEvent.setup()

    // open
    const openButton = await canvas.findByRole("button", { name: "Confirmation Modal" })
    await user.click(openButton)

    // use screen as we are querying outside the canvas (portal)
    const dialog = await screen.findByRole("dialog", { name: "Confirmation Modal" })
    expect(dialog).toBeInTheDocument()

    // press escape
    await user.keyboard("{Escape}")

    // wait for it to unmount / finish exit animation
    await waitForElementToBeRemoved(dialog)

    // only onCancel should be called when closing
    expect(args.onConfirm).not.toHaveBeenCalled()
    expect(args.onCancel).toHaveBeenCalled()
  },
}

export const WithChildren: Story = {
  args: {
    children: MockForm(),
  },
}

export const WithConfirmTextOverride: Story = {
  args: {
    children: MockForm(),
    confirmTextOverride: "Proceed",
  },
}

export const WithLoading: Story = {
  args: {
    children: MockForm(),
    loading: true,
  },
}

export const WithMessage: Story = {
  args: {
    title: "Delete Document Definition",
    message: "Are you sure you want to delete 'Test Doc Def 1'?",
  },
}
