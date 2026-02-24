import { FC } from "react"
import type { Meta, StoryObj } from "@storybook/react"
import { Group, List, Stack, Text, TextInput, Title as MantineTitle } from "@mantine/core"
import { PageHeader } from "../PageHeader/PageHeader"
import { Title } from "../Title/Title"
import { Button } from "../Button/Button"
import { Icon } from "../Icon/Icon"
import { Header } from "../Header/Header"
import { Sidebar } from "../Sidebar/Sidebar"
import { SideDrawerTabbed } from "../SideDrawerTabbed/SideDrawerTabbed"
import { sidebarNavItems } from "../../../storybook/mixins"
import { SideDrawer } from "../SideDrawer/SideDrawer"
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
import MockLogoLightMode from "../../../storybook/assets/autoform-dm-symbol.svg"
import MockLogoDarkMode from "../../../storybook/assets/autoform-dm-symbol-white.svg"
import { AppLayout } from "./AppLayout"
import {
  GridExample3Items,
  GridExample6Items,
  SimpleGridExample3Items,
  SimpleGridExample6Items,
  SimpleGridExample9Items,
} from "../ActionCard/ActionCard.stories"
import { UserCardGrid } from "../UserCard/UserCard.stories"
import { FormExample } from "../forms/Forms.stories"
import { CheckboxCardGroup } from "../CheckboxCardGroup/CheckboxCardGroup"
import { CheckboxCard } from "../CheckboxCard/CheckboxCard"
import { useSideDrawer } from "../../hooks/useSideDrawer"

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

const SideDrawerTabbedButton: FC = () => {
  const { toggle, close } = useSideDrawer("tabbed", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Side Drawer Tabbed
      </Button>
      <SideDrawerTabbed
        name={"tabbed"}
        location={"right"}
        expandable
        pinnable
        tabs={[
          {
            id: "tab1",
            title: "Tab 1 Title",
            content: <Text>Tab 1 Content</Text>,
            footer: {
              content: (
                <Group grow w={"100%"}>
                  <Text>Tab 1 Footer</Text>
                </Group>
              ),
              subtle: true,
            },
          },
          { id: "tab2", title: "Tab 2 Title", content: <Text>Tab 2 Content</Text> },
          {
            id: "tab3",
            title: "Tab 3 Longer Title",
            content: (
              <Stack>
                {Array.from({ length: 25 }, (_, i) => (
                  <Text>Tab 3 Content {i + 1}</Text>
                ))}
              </Stack>
            ),
            footer: {
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
              subtle: false,
            },
          },
        ]}
      />
    </>
  )
}

const SideDrawerCheckboxCardButton: FC = () => {
  const { toggle } = useSideDrawer("checkbox-cards", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Drawer With Checkbox Cards
      </Button>
      <SideDrawer name={"checkbox-cards"} location={"right"} title={"Side Drawer"} pinnable>
        <Stack>
          <CheckboxCard stacked label="Tradernet Checkbox Card" description="Tradernet Checkbox Card description example text" />
          <CheckboxCard stacked label="Tradernet Checkbox Card" description="Tradernet Checkbox Card description example text" />
          <CheckboxCard stacked label="Tradernet Checkbox Card" description="Tradernet Checkbox Card description example text" />
        </Stack>
      </SideDrawer>
    </>
  )
}

const ManageDrawerButton: FC = () => {
  const { toggle } = useSideDrawer("manage", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Manage Drawer
      </Button>
      <SideDrawerTabbed
        name={"manage"}
        location={"right"}
        expandable
        pinnable
        tabs={[
          { id: "tab1", title: "Preview", content: <Text>Preview Content</Text> },
          { id: "tab2", title: "Metadata", content: <Text>Metadata Content</Text> },
          { id: "tab3", title: "JobInfos", content: <Text>JobInfos Content</Text> },
          { id: "tab4", title: "Item Lines", content: <Text>Item Lines Content</Text> },
          { id: "tab5", title: "Attachments", content: <Text>Attachments Content</Text> },
        ]}
      />
    </>
  )
}

const KeepDrawerButton: FC = () => {
  const { toggle } = useSideDrawer("keep", "right")
  return (
    <>
      <Button variant={"outline"} onClick={toggle}>
        Keep Drawer
      </Button>
      <SideDrawerTabbed
        name={"keep"}
        location={"right"}
        expandable
        tabs={[
          { id: "tab1", title: "Preview", content: <Text>Preview Content</Text> },
          { id: "tab2", title: "Key Data", content: <Text>Key Data Content</Text> },
          { id: "tab3", title: "Line Items", content: <Text>Line Items Content</Text> },
          { id: "tab4", title: "Notes", content: <Text>Notes Content</Text> },
        ]}
      />
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
        leftSection={[<TextInput variant={"outline"} leftSection={<Icon name={"magnifying-glass"} />} placeholder={"Search..."} />]}
        rightSection={[<SideDrawerWithFooterButton />, <Button>Main Button</Button>]}
        secondaryActions={[
          { icon: "gears", label: "Settings", onClick: () => alert("Settings clicked") },
          { icon: "info", label: "Information", onClick: () => alert("Information clicked") },
        ]}
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
          <Header<MockLinkProps>
            LinkComponent={MockLink}
            linkPropName={mockLinkPropName}
            logoLinkPath={"/home"}
            logoLightModeSrc={MockLogoLightMode}
            logoDarkModeSrc={MockLogoDarkMode}
            rightSection={MockHeaderSignIn}
          />
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

export const SideDrawerWithTabs: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SideDrawerTabbedButton />,
  },
}

export const SideDrawerWithCheckboxCards: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SideDrawerCheckboxCardButton />,
  },
}

export const SideDrawersForProducts: Story = {
  args: {
    sidebar: "None",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: (
      <Stack>
        <Text>Do all the required tabs fit?</Text>
        <Group>
          <KeepDrawerButton />
          <ManageDrawerButton />
        </Group>
      </Stack>
    ),
  },
}

export const With3Cards: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: (
      <Stack>
        <GridExample3Items />
      </Stack>
    ),
  },
}

export const With6Cards: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: (
      <Stack>
        <GridExample6Items />
      </Stack>
    ),
  },
}

export const With3CardsGrid: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SimpleGridExample3Items />,
  },
}

export const With6CardsGrid: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SimpleGridExample6Items />,
  },
}

export const With9CardsGrid: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <SimpleGridExample9Items />,
  },
}

export const WithUserCardsGrid: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: <UserCardGrid />,
  },
}

export const WithFormElements: Story = {
  args: {
    sidebar: "MockSidebar",
    footer: "None",
    header: "MockHeaderActionIcons",
    children: (
      <Stack>
        <FormExample />
        <CheckboxCardGroup>
          <CheckboxCard label="Tradernet Checkbox Card 1" description="Tradernet Checkbox Card description example text" />
          <CheckboxCard
            label="Tradernet Checkbox Card 2"
            description="Tradernet Checkbox Card description example text but this one is a lot longer so it wraps onto two lines"
          />
          <CheckboxCard label="Tradernet Checkbox Card 3" description="Tradernet Checkbox Card description example text" />
          <CheckboxCard label="Tradernet Checkbox Card 4" description="Tradernet Checkbox Card description example text" />
        </CheckboxCardGroup>
      </Stack>
    ),
  },
}
