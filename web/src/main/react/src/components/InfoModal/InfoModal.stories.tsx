import type { Meta, StoryObj } from "@storybook/react"
import { InfoModal } from "./InfoModal"
import { fn } from "@storybook/test"
import { CopyButton } from "@mantine/core"
import { Button } from "../Button/Button"
import { useState } from "react"

// @ts-ignore
const meta = {
  component: InfoModal,
  title: "Tradernet/Modals/InfoModal",
  args: {
    title: "Info Modal",
    opened: true,
    onClose: fn(),
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
          Info Modal
        </Button>
        <InfoModal
          {...args}
          opened={open}
          onClose={() => {
            args.onClose()
            setOpen(false)
          }}
        />
      </>
    )
  },
} satisfies Meta<typeof InfoModal>

export default meta

type Story = StoryObj<typeof InfoModal>

export const Default: Story = {}

const MockChildren = () => {
  const mockAuditData = `{
    eventId: 1,
    eventTime: "2025-08-18T15:11:10.005+00:00",
    eventType: "login",
    eventSrcIp: "172.18.0.1",
    eventSrcEndpoint: "REST",
    eventUser: 1,
    eventUsername: "admin",
    eventTimeDisplay: "8/18/2025, 4:11:10.005 PM",
  }`

  return (
    <>
      <pre style={{ fontSize: 14 }}>{mockAuditData}</pre>
      <CopyButton value={mockAuditData} timeout={2000}>
        {({ copied, copy }) => (
          <Button leftIcon={"copy"} onClick={copy} variant={"outline"} size={"sm"}>
            {copied ? "Copied" : "Copy"}
          </Button>
        )}
      </CopyButton>
    </>
  )
}

export const WithChildren: Story = {
  args: {
    title: "Full Audit Data",
    children: MockChildren(),
  },
}

export const WithMessage: Story = {
  args: {
    title: "For your information",
    message: "This is some useful information. Bet you didn't know that.",
  },
}
