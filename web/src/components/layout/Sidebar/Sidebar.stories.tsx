import type { Meta, StoryObj } from "@storybook/react"
import { expect, userEvent, within } from "@storybook/test"
import { Box } from "@mantine/core"
import Sidebar from "./Sidebar"
import { sidebarNavItems } from "../../../storybook/mixins"
import { mockBottomLinkItem, MockLink, mockLinkPropName, MockLinkProps, MockRouterProvider, useMockRouter } from "../../../storybook/mocks"

const meta = {
  title: "Tradernet/Sidebar",
  component: Sidebar,
  args: {
    items: sidebarNavItems.slice(0, 5),
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    activePath: "/home",
    bottomLinkItem: mockBottomLinkItem,
  },
  argTypes: {
    items: {
      control: { type: "select" },
      description: "Sidebar items to be rendered",
      options: ["MenuItems", "MenuItemsWithSubMenus", "LongMenuItemsList"],
      mapping: {
        MenuItems: sidebarNavItems.slice(0, 5),
        MenuItemsWithSubMenus: sidebarNavItems.slice(5, 10),
        LongMenuItemsList: sidebarNavItems,
      },
      table: {
        type: { summary: "FormpipeSidebarItem[]" },
        category: "Customisable",
      },
    },
    activePath: {
      description: "Active path string, used for indicating the currently active nav item",
      table: {
        category: "Customisable",
      },
    },
    LinkComponent: {
      control: false, // Disable control for LinkComponent as it's defined automatically by the passed component
      description:
        'Implementation specific component to be used as Link component for nav items, i.e. Next.js Router\'s "Link" component, or React Router\'s "Link" component',
      table: {
        category: "Implementation Specific",
      },
    },
    linkPropName: {
      control: false, // Disable control for linkNameProp as it's tied to the passed LinkComponent's "path" prop name
      description:
        'Implementation specific Link component\'s "path" prop name, i.e. Next.js Router\'s "Link" component uses "href", whereas, React Router\'s "Link" component uses "to"',
      table: {
        category: "Implementation Specific",
      },
    },
    bottomLinkItem: {
      control: { type: "select" },
      description: 'Bottom link item to be rendered at bottom of sidebar, intended to be used for items such as "System Settings"',
      options: ["None", "MockBottomLinkItem"],
      mapping: {
        None: undefined,
        MockBottomLinkItem: mockBottomLinkItem,
      },
      table: {
        type: { summary: "FormpipeSidebarItem" },
        category: "Customisable",
      },
    },
  },
  decorators: [
    (Story, ctx) => (
      <Box h={700} w={300} style={{ overflow: "hidden" }}>
        <MockRouterProvider activePath={ctx.args.activePath}>
          <Story />
        </MockRouterProvider>
      </Box>
    ),
  ],
} satisfies Meta<typeof Sidebar<MockLinkProps>>

export default meta
type Story = StoryObj<typeof Sidebar<MockLinkProps>>

/**
 * Default
 */
export const Default: Story = {
  args: {
    items: sidebarNavItems.slice(0, 5),
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    activePath: "/home",
    bottomLinkItem: mockBottomLinkItem,
  },
  decorators: [
    (Story, ctx) => {
      const { activePath } = useMockRouter()
      return <Story args={{ ...ctx.args, activePath }} />
    },
  ],
}

/**
 * With Menu Scroll
 */
export const WithMenuScroll: Story = {
  args: {
    items: sidebarNavItems,
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    activePath: "/home",
    bottomLinkItem: undefined,
  },
  decorators: [
    (Story, ctx) => {
      const { activePath } = useMockRouter()
      return <Story args={{ ...ctx.args, activePath }} />
    },
  ],
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)

    /** check that the active link has the correct "active" attribute */
    const initialActiveLink = canvas.getByTestId(`${args.items[0].label}-menu-item`)
    expect(initialActiveLink).toHaveAttribute("data-active", "true")

    /** ensure link clicks are updating the active path correctly */
    const testLink = canvas.getByTestId(`${args.items[3].label}-menu-item`)
    await userEvent.click(testLink)
    expect(testLink).toHaveAttribute("data-active", "true")

    /** ensure Sidebar expansion is working */
    const expandButton = canvas.getByTestId(`icon-arrow-right-from-line`)
    await userEvent.click(expandButton)
    args.items.forEach((sidebarNavItem) => expect(canvas.getByText(sidebarNavItem.label)).toBeVisible())

    /** in expanded mode, the currently active link should have all sub menu items displayed */
    args.items[3].subItems?.forEach((sidebarNavSubItem) => expect(canvas.getByText(sidebarNavSubItem.label)).toBeVisible())

    /**
     * when an item is clicked that does not have a path, it's first sub menu item should be activated instead,
     * with item and sub item active state being updated accordingly
     */
    const testNoPathItem = canvas.getByTestId(`${args.items[1].label}-menu-item`)
    await userEvent.click(testNoPathItem)
    expect(testNoPathItem).toHaveAttribute("data-active", "false")
    const testSubMenuItem = canvas.getByTestId(`${args.items[1].subItems?.[0].label}-sub-menu-item`)
    expect(testSubMenuItem).toHaveAttribute("data-active", "true")
  },
}

/**
 * With Sub Menu
 */
export const WithSubMenu: Story = {
  args: {
    items: sidebarNavItems.slice(0, 5),
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    activePath: "/all-users",
    bottomLinkItem: undefined,
  },
  decorators: [
    (Story, ctx) => {
      const { activePath } = useMockRouter()
      return <Story args={{ ...ctx.args, activePath }} />
    },
  ],
}

/**
 * Sub Menu Item Active
 */
export const SubMenuItemActive: Story = {
  args: {
    items: sidebarNavItems.slice(0, 5),
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    activePath: "/logout",
    bottomLinkItem: undefined,
  },
  decorators: [
    (Story, ctx) => {
      const { activePath } = useMockRouter()
      return <Story args={{ ...ctx.args, activePath }} />
    },
  ],
}

/**
 * With Bottom Link Item
 */
export const WithBottomLinkPath: Story = {
  args: {
    items: sidebarNavItems.slice(0, 5),
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    activePath: "/logout",
    bottomLinkItem: mockBottomLinkItem,
  },
  decorators: [
    (Story, ctx) => {
      const { activePath } = useMockRouter()
      return <Story args={{ ...ctx.args, activePath }} />
    },
  ],
}
