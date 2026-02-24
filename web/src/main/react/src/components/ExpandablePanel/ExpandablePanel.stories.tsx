import { ReactNode, useState } from "react"
import { Story } from "@storybook/blocks"
import type { Meta, StoryObj } from "@storybook/react"
import { expect, fn, screen, userEvent, waitFor, within } from "@storybook/test"
import { ExpandablePanel, PanelAction } from "./ExpandablePanel"
import { Flex, Group, Select, Text } from "@mantine/core"
import { Button } from "../Button/Button"

/** Mock child component */
const MockChild: ReactNode = (
  <Flex justify="center" direction="column">
    <Text size="xl">Tradernet Expandable Panel Mock Content</Text>
    <Text size="sm">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</Text>
  </Flex>
)

/** Mock Actions */
const mockActions: PanelAction[] = [
  {
    label: "Test Action 1",
    icon: "arrow-down",
    callback: fn(),
  },
  {
    label: "Test Action 2",
    icon: "arrow-up",
    callback: fn(),
  },
  {
    label: "Test Action 3",
    icon: "trash-can",
    callback: fn(),
  },
]

/** Mock Action Buttons */
const mockActionButtons: PanelAction[] = [
  {
    label: "Button 1",
    icon: "plus",
    callback: fn(),
  },
  {
    label: "Button 2",
    icon: "graduation-cap",
    callback: fn(),
  },
]

/** Mock centre content with control elements */
const mockControlsCentreContent: ReactNode = (
  <Group>
    <Select data={["AND", "OR"]} defaultValue="AND" maw={100} />
    {mockActionButtons.map((actionButton: PanelAction) => (
      <Button size="sm" variant="outline" aria-label={`Tradernet Expandable Panel-${actionButton.label}-panel-button`} leftIcon={actionButton.icon}>
        {actionButton.label}
      </Button>
    ))}
  </Group>
)

/** Mock centre content with text elements */
const mockTextCentreContent: ReactNode = (
  <Group>
    <Text>[input 1]</Text>
    <Text>[document id]</Text>
  </Group>
)

const meta = {
  title: "Tradernet/ExpandablePanel",
  component: ExpandablePanel,
  args: {
    title: "Tradernet Expandable Panel",
    subtitle: "",
    isOpen: false,
    icon: undefined,
    toggleOpenOnGrid: false,
    actions: [],
    showActionTooltips: false,
    panelCentreContent: undefined,
    disabled: false,
    width: "100%",
    collapseOnClickAway: false,
    showCheckbox: false,
    isChecked: false,
    onCheckboxChange: undefined,
    children: MockChild,
  },
  argTypes: {
    title: {
      control: { type: "text" },
      description: "Displayed panel title",
      table: {
        type: { summary: "string" },
      },
    },
    subtitle: {
      control: { type: "text" },
      description: "Displayed panel subtitle, if provided",
      table: {
        type: { summary: "string" },
      },
    },
    isOpen: {
      control: { type: "boolean" },
      description: "External control for setting panel open/closed",
      table: {
        type: { summary: "boolean" },
      },
    },
    icon: {
      control: { type: "select" },
      // options: [undefined, ...Object.values(IconType)],
      description: "The icon to display next to the title",
      table: {
        type: { summary: "ReactNode" },
      },
    },
    toggleOpenOnGrid: {
      control: { type: "boolean" },
      description: "Toggle open/close panel on grid area click",
      table: {
        type: { summary: "boolean" },
      },
    },
    actions: {
      control: { type: "select" },
      description: "List of action icons to be displayed on the right hand side of panel",
      options: ["None", "MockActions"],
      mapping: {
        None: undefined,
        MockActions: mockActions,
      },
      table: {
        type: { summary: "PanelAction[]", detail: `{ label: string, iconType: IconType, callback: () => void }[]` },
      },
    },
    showActionTooltips: {
      control: { type: "boolean" },
      description: "Show tooltips for panel actions",
      table: {
        type: { summary: "boolean" },
      },
    },
    panelCentreContent: {
      control: { type: "select" },
      description: "Component to be rendered in centre of panel next to panel title",
      options: ["None", "MockCentreContentControls", "MockCentreContentText"],
      mapping: {
        None: undefined,
        MockCentreContentControls: mockControlsCentreContent,
        MockCentreContentText: mockTextCentreContent,
      },
      table: {
        type: { summary: "ReactNode" },
      },
    },
    disabled: {
      control: { type: "boolean" },
      description: "Is panel open/close functionality disabled",
      table: {
        type: { summary: "boolean" },
      },
    },
    width: {
      control: { type: "select" },
      description: "Defines the width of the panel",
      options: [undefined, "auto", "100%", "30rem", "300px", 400],
      table: {
        type: { summary: "TradernetWidth", detail: `"auto" | \`\${number}%\` | \`\${number}px\` | \`\${number}rem\` | number` },
      },
    },
    collapseOnClickAway: {
      control: { type: "boolean" },
      description: "Does the panel close when user clicks away",
      table: {
        type: { summary: "booleans" },
      },
    },
    showCheckbox: {
      control: { type: "boolean" },
      description: "Does the panel display a checkbox to the left of panel title",
      table: {
        type: { summary: "boolean" },
        category: "Checkbox",
      },
    },
    isChecked: {
      control: { type: "boolean" },
      description: "Is the panel checkbox currently checked",
      table: {
        type: { summary: "boolean" },
        category: "Checkbox",
      },
    },
    onCheckboxChange: {
      description: "Callback handler for panel checkbox change",
      table: {
        type: { summary: "(value: boolean) => void" },
        category: "Checkbox",
      },
    },
    children: {
      control: { type: "select", labels: { None: undefined } },
      options: ["None", "MockChild"],
      mapping: {
        None: undefined,
        MockChild: MockChild,
      },
      description: "The panel content to be displayed when expanded",
      table: {
        type: { summary: "ReactNode" },
      },
    },
  },
  render: (args) => {
    const [mockIsSelected, setIsMockSelected] = useState<boolean>(args.isChecked ?? false)
    return <ExpandablePanel {...args} isChecked={mockIsSelected} onCheckboxChange={(value) => setIsMockSelected(value)} />
  },
} satisfies Meta<typeof ExpandablePanel>

export default meta
type Story = StoryObj<typeof ExpandablePanel>

/**
 *  Default story
 */
export const Default: Story = {
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement)
    const dropdownArrow = canvas.getByLabelText(`${args.title}-panel-button`)

    /** Panel should be expanded after click on dropdown arrow */
    await userEvent.click(dropdownArrow)
    await expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute("aria-hidden", "false")

    /** Wait for expand animation to finish (ensures consistent snapshot) */
    await waitFor(() => {
      expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute("style", "box-sizing: border-box; opacity: 1; transition: opacity 200ms;")
    })
  },
}

/**
 *  Disabled story
 */
export const Disabled: Story = {
  args: {
    disabled: true,
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement)
    const dropdownArrow = canvas.getByLabelText(`${args.title}-panel-button`)

    /** Panel should not expand after click on dropdown arrow */
    await userEvent.click(dropdownArrow)
    await expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute("aria-hidden", "true")
  },
}

/**
 *  With Subtitle story
 */
export const WithSubtitle: Story = {
  args: {
    subtitle: "Tradernet Expandable Panel Subtitle",
  },
}

/**
 *  Long Title story
 */
export const LongTitle: Story = {
  args: {
    title: "Tradernet Expandable Panel With An Extremely Long Title To See How The Component Renders",
  },
}

/**
 *  With Long Subtitle story
 */
export const WithLongSubtitle: Story = {
  args: {
    title: "Tradernet Expandable Panel With An Extremely Long Title To See How The Component Renders",
    subtitle:
      "Tradernet Expandable Panel With An Extremely Long Subtitle To See How The Component Renders Tradernet Expandable Panel With An Extremely Long Subtitle To See How The Component Renders",
  },
}

/**
 *  With Grid area on click story
 */
export const WithGridAreaOnClick: Story = {
  args: {
    title: "Tradernet Expandable Panel With the an OnClick event in the grid area",
    toggleOpenOnGrid: true,
  },
}

/**
 *  Fixed Pixel Width story
 */
export const FixedPixelWidth: Story = {
  args: {
    width: "300px",
  },
}

/**
 *  Fixed REM Width story
 */
export const FixedRemWidth: Story = {
  args: {
    width: "30rem",
  },
}

/**
 *  Fixed % Width story
 */
export const FixedPercentWidth: Story = {
  args: {
    width: "50%",
  },
}

/**
 * With Panel Icon story
 */
export const WithPanelIcon: Story = {
  args: {
    icon: "key",
  },
}

/**
 *  Selectable story
 */
export const Selectable: Story = {
  args: {
    showCheckbox: true,
    isChecked: true,
  },
}

/**
 *  Externally Opened story
 */
export const ExternallyOpened: Story = {
  args: {
    isOpen: true,
  },
}

/**
 *  With Actions story
 */
export const WithActions: Story = {
  args: {
    actions: mockActions,
    showActionTooltips: true,
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement)
    const popoverButton = canvas.getByLabelText(`${args.title}-panel-popover-action`)

    /** Popover content should be displayed after click on button */
    await userEvent.click(popoverButton)
    await waitFor(() => {
      expect(screen.queryByLabelText(`${args.title}-panel-popover-content`)).toBeTruthy()
    })
    args.actions?.forEach((action) => expect(screen.queryByText(`${action.label}`)).toBeTruthy())

    /** Panel should still be collapsed */
    await expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute("aria-hidden", "true")

    /** Popover should not be shown after second click on button */
    await userEvent.click(popoverButton)
    await waitFor(() => {
      expect(screen.queryByLabelText(`${args.title}-panel-popover-content`)).toBeNull()
    })
    args.actions?.forEach((action) => expect(screen.queryByText(`${action.label}`)).toBeNull())

    /** Popover should reopen on third click */
    await userEvent.click(popoverButton)
    await waitFor(() => {
      expect(screen.queryByLabelText(`${args.title}-panel-popover-content`)).toBeTruthy()
    })
    args.actions?.forEach((action) => expect(screen.queryByText(`${action.label}`)).toBeTruthy())

    /** Clicking on action item 1 should invoke callback and close popover */
    const actionOneItem = screen.getByText(`${args.actions && args.actions[0].label}`)
    await userEvent.click(actionOneItem)
    await expect(args.actions && args.actions[0].callback).toHaveBeenCalled()
    await waitFor(() => {
      expect(screen.queryByLabelText(`${args.title}-panel-popover-content`)).toBeNull()
    })
    args.actions?.forEach((action) => expect(screen.queryByText(`${action.label}`)).toBeNull())

    /** Wait for collapse animation to finish (ensures consistent snapshot) */
    await waitFor(() => {
      expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute(
        "style",
        "box-sizing: border-box; opacity: 0; transition: opacity 200ms; display: none; height: 0px; overflow: hidden;",
      )
    })
  },
}

/**
 *  With Panel Central Controls Content story
 */
export const WithPanelCentralControlsContent: Story = {
  args: {
    panelCentreContent: mockControlsCentreContent,
  },
}

/**
 *  With Panel Central Text Content story
 */
export const WithPanelCentralTextContent: Story = {
  args: {
    panelCentreContent: mockTextCentreContent,
  },
}

/**
 *  Collapse On Click Away story
 */
export const CollapseOnClickAway: Story = {
  args: {
    collapseOnClickAway: true,
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement)
    const dropdownArrow = canvas.getByLabelText(`${args.title}-panel-button`)

    /** Panel should be expanded after click on dropdown arrow */
    await userEvent.click(dropdownArrow)
    await expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute("aria-hidden", "false")

    /** Panel should be collapsed after click away */
    await userEvent.click(document.body)
    await expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute("aria-hidden", "true")

    /** Wait for collapse animation to finish (ensures consistent snapshot) */
    await waitFor(() => {
      expect(canvas.queryByLabelText(`${args.title}-panel-content`)).toHaveAttribute(
        "style",
        "box-sizing: border-box; opacity: 0; transition: opacity 200ms; display: none; height: 0px; overflow: hidden;",
      )
    })
  },
}

/**
 *  With All Options story
 */
export const WithAllOptions: Story = {
  args: {
    icon: "key",
    collapseOnClickAway: true,
    showCheckbox: true,
    isChecked: true,
    actions: mockActions,
    panelCentreContent: mockControlsCentreContent,
  },
}

/**
 *  No Children story
 */
export const NoChildren: Story = {
  args: {
    children: undefined,
  },
}
