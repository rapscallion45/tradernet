import { FC } from "react"
import type { Meta, StoryObj } from "@storybook/react"
import { Group, List, Stack, Text, TextInput, Title as MantineTitle } from "@mantine/core"
import { IconSearch } from "@tabler/icons-react"
import AppLayout from "components/layout/AppLayout/AppLayout"
import PageHeader from "components/layout/PageHeader/PageHeader"
import Title from "components/Title/Title"
import Button from "components/Button/Button"
import Header from "components/layout/Header/Header"
import Sidebar from "components/layout/Sidebar/Sidebar"
import SideDrawer from "components/layout/SideDrawer/SideDrawer"
import { sidebarNavItems } from "../../../storybook/mixins"
import {
  mockBottomLinkItem,
  MockBreadcrumbs,
  MockHeaderActions,
  MockHeaderSignIn,
  MockLink,
  mockLinkPropName,
  MockLinkProps,
  MockRouterProvider,
  useMockRouter,
} from "../../../storybook/mocks"
import { useSideDrawer } from "hooks/useSideDrawer"

/** Mock footer */
const MockFooter: FC = () => (
  <Group h={"100%"} px={40} style={{ alignContent: "center" }}>
    <Text size={"xs"}>Test footer | software version: X.XX.XX</Text>
  </Group>
)

const DrawerContent = () => (
  <Stack gap={"md"}>
    {Array.from({ length: 25 }, (_, i) => (
      <Text ff={"monospace"}>Side Drawer Content {i + 1}</Text>
    ))}
  </Stack>
)

const SideDrawerSimpleButton: FC = () => {
  const { toggle } = useSideDrawer("simple", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Drawer Button
      </Button>
      <SideDrawer name={"simple"} location={"right"} title={"Side Drawer"}>
        <DrawerContent />
      </SideDrawer>
    </>
  )
}

const SideDrawerWithFooterButton: FC = () => {
  const { toggle, close } = useSideDrawer("with-footer", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Drawer On Right
      </Button>
      <SideDrawer
        name={"with-footer"}
        location={"right"}
        title={"Side Drawer"}
        pinnable
        footer={{
          content: (
            <Group grow w={"100%"}>
              <Button variant={"outline"} onClick={close}>
                Reset
              </Button>
              <Button variant={"filled"} onClick={close}>
                Close
              </Button>
            </Group>
          ),
        }}>
        <DrawerContent />
      </SideDrawer>
    </>
  )
}

const SideDrawerWithSubtleHeaderAndFooterButton: FC = () => {
  const { toggle, close } = useSideDrawer("with-subtle-header-footer", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Drawer With Subtle Header And Footer
      </Button>
      <SideDrawer
        name={"with-subtle-header-footer"}
        location={"right"}
        title={"Side Drawer"}
        subtleHeader
        footer={{
          content: (
            <Group grow w={"100%"}>
              <Button variant={"outline"} onClick={close}>
                Reset
              </Button>
              <Button variant={"filled"} onClick={close}>
                Close
              </Button>
            </Group>
          ),
          subtle: true,
        }}>
        <DrawerContent />
      </SideDrawer>
    </>
  )
}

const SideDrawerOnLeftButton: FC = () => {
  const { toggle } = useSideDrawer("left", "left")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Drawer On Left
      </Button>
      <SideDrawer name={"left"} location={"left"} title={"Side Drawer"} pinnable>
        <DrawerContent />
      </SideDrawer>
    </>
  )
}

/** Mock page content */
const MockPage: FC = () => {
  const { activePath } = useMockRouter()
  const pageTitle = activePath.split("/").pop()?.replace("-", " ") ?? ""
  return (
    <Stack>
      <PageHeader
        title={<Title highlight={1}>{pageTitle.charAt(0).toUpperCase() + pageTitle.slice(1)}</Title>}
        leftSection={[<TextInput variant={"outline"} leftSection={<IconSearch />} placeholder={"Search..."} />]}
        rightSection={[<SideDrawerWithFooterButton />, <Button>Main Button</Button>]}
        hiddenSection={[<Button variant={"subtle"}>And Another Button</Button>, <Button variant={"subtle"}>And Yet Another Button</Button>]}
      />
      <List>
        {Array.from({ length: 50 }, (_, i) => (
          <List.Item>Glorious Content {i + 1}</List.Item>
        ))}
      </List>
    </Stack>
  )
}

const meta = {
  title: "Tradernet/AppLayout",
  component: AppLayout,
  parameters: {
    layout: "center",
  },
  args: {
    header: "MockHeaderActionIcons",
    sidebar: "MockSidebar",
    footer: "MockFooter",
    children: "MockPage",
  },
  argTypes: {
    header: {
      control: { type: "select" },
      description: "Header component to be rendered",
      options: ["MockHeaderActionIcons", "MockHeaderSignIn"],
      mapping: {
        MockHeaderActionIcons: (
          <Header<MockLinkProps>
            LinkComponent={MockLink}
            linkPropName={mockLinkPropName}
            logoLinkPath={"/home"}
            title={<MantineTitle order={3}>Keep</MantineTitle>}
            leftSection={<MockBreadcrumbs />}
            rightSection={MockHeaderActions}
          />
        ),
        MockHeaderSignIn: (
          <Header<MockLinkProps> LinkComponent={MockLink} linkPropName={mockLinkPropName} logoLinkPath={"/home"} rightSection={MockHeaderSignIn} />
        ),
      },
      table: {
        type: { summary: "ReactNode" },
      },
    },
    sidebar: {
      control: { type: "select" },
      description: "Sidebar component to be rendered",
      options: ["None", "MockSidebar"],
      mapping: {
        None: undefined,
        MockSidebar: (
          <Sidebar<MockLinkProps>
            LinkComponent={MockLink}
            linkPropName={mockLinkPropName}
            activePath={"/home"}
            items={sidebarNavItems}
            bottomLinkItem={mockBottomLinkItem}
          />
        ),
      },
      table: {
        type: { summary: "ReactNode" },
      },
    },
    footer: {
      control: { type: "select" },
      description: "Footer component to be rendered",
      options: ["None", "MockFooter"],
      mapping: {
        None: undefined,
        MockFooter: <MockFooter />,
      },
      table: {
        type: { summary: "ReactNode" },
      },
    },
    children: {
      control: { type: "select" },
      options: ["None", "MockPage"],
      mapping: {
        None: undefined,
        MockPage: <MockPage />,
      },
      description: "The page content to be displayed within the App Shell",
      table: {
        type: { summary: "ReactNode" },
      },
    },
  },
  decorators: [
    (Story) => {
      return (
        <MockRouterProvider activePath={"/home"}>
          <Story />
        </MockRouterProvider>
      )
    },
  ],
} satisfies Meta<typeof AppLayout>

export default meta
type Story = StoryObj<typeof AppLayout>

/**
 * All Elements
 */
export const AllElements: Story = {
  args: {
    header: "MockHeaderActionIcons",
    sidebar: "MockSidebar",
    footer: "MockFooter",
    children: "MockPage",
  },
}

/**
 * Header Only
 */
export const HeaderOnly: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderSignIn",
    children: "MockPage",
  },
}

/**
 * Header And Sidebar
 */
export const HeaderAndSidebar: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: "MockPage",
  },
  decorators: [
    (Story, ctx) => {
      const { activePath } = useMockRouter()
      return (
        <Story
          args={{
            ...ctx.args,
            sidebar: ctx.args.sidebar ? (
              <Sidebar
                LinkComponent={MockLink}
                linkPropName={mockLinkPropName}
                activePath={activePath}
                items={sidebarNavItems.slice(0, 5)}
                bottomLinkItem={mockBottomLinkItem}
              />
            ) : null,
          }}
        />
      )
    },
  ],
}

/**
 * Header And Footer
 */
export const HeaderAndFooter: Story = {
  args: {
    sidebar: "None",
    footer: "MockFooter",
    header: "MockHeaderSignIn",
    children: "MockPage",
  },
}

export const SideDrawerSimple: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SideDrawerSimpleButton />,
  },
}

export const SideDrawerWithFooter: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SideDrawerWithFooterButton />,
  },
}

export const SideDrawerOnLeft: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: (
      <Group>
        <SideDrawerOnLeftButton />
        <SideDrawerWithFooterButton />
      </Group>
    ),
  },
}

export const SideDrawerWithSubtle: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SideDrawerWithSubtleHeaderAndFooterButton />,
  },
}
