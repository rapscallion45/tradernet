import type { Meta, StoryObj } from "@storybook/react"
import { BaseModal } from "./BaseModal"
import { expect, fn, userEvent, within, screen, waitForElementToBeRemoved } from "@storybook/test"
import { useState } from "react"
import { Button } from "../Button/Button"

const meta = {
  component: BaseModal,
  title: "Tradernet/Modals/BaseModal",
  args: {
    title: "Base Modal",
  },
  render: (args) => {
    const [open, setOpen] = useState(false)
    return (
      <>
        <Button onClick={() => setOpen(true)} variant="outline">
          Base Modal
        </Button>
        <BaseModal {...args} opened={open} onClose={() => setOpen(false)} />
      </>
    )
  },
} satisfies Meta<typeof BaseModal>

export default meta

type Story = StoryObj<typeof BaseModal>

export const Default: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const user = userEvent.setup()

    // open
    const openButton = await canvas.findByRole("button", { name: "Base Modal" })
    await user.click(openButton)

    // use screen as we are querying outside the canvas (portal)
    const dialog = await screen.findByRole("dialog", { name: "Base Modal" })
    expect(dialog).toBeInTheDocument()

    // close
    const close = await screen.findByRole("button", { name: "Close" })
    await user.click(close)

    // wait for it to unmount / finish exit animation
    await waitForElementToBeRemoved(dialog)
  },
}
