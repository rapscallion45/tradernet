import type { Meta, StoryObj } from "@storybook/react"
import { Box, Title } from "@mantine/core"
import { Header } from "./Header"
import { MockBreadcrumbs, MockHeaderActions, MockHeaderSignIn, MockRouterProvider, MockLink, mockLinkPropName, MockLinkProps } from "../../../storybook/mocks"
import DMLogoLightMode from "../../../storybook/assets/autoform-dm-symbol.svg"
import DMLogoDarkMode from "../../../storybook/assets/autoform-dm-symbol-white.svg"
import PortalLogoLightMode from "../../../storybook/assets/lasernet-online-symbol.jpg"

const meta = {
  title: "Tradernet/Header",
  component: Header,
  args: {
    logoLightModeSrc: DMLogoLightMode,
    logoDarkModeSrc: DMLogoDarkMode,
    LinkComponent: MockLink,
    linkPropName: mockLinkPropName,
    logoLinkPath: "/",
    logoAlt: "Tradernet Logo",
  },
  argTypes: {
    logoLightModeSrc: {
      control: { type: "select" },
      description: 'Logo image to be displayed right of Tradernet "crest" logo in light mode',
      options: ["None", "DMLightModeLogo", "PortalLightModeLogo"],
      mapping: {
        None: undefined,
        DMLightModeLogo: DMLogoLightMode,
        PortalLightModeLogo: PortalLogoLightMode,
      },
      table: {
        type: { summary: "ReactNode" },
        category: "Customisable",
      },
    },
    logoDarkModeSrc: {
      control: { type: "select" },
      description: 'Logo image to be displayed right of Tradernet "crest" logo in dark mode',
      options: ["None", "DMDarkModeLogo"],
      mapping: {
        None: undefined,
        DMDarkModeLogo: DMLogoDarkMode,
      },
      table: {
        type: { summary: "ReactNode" },
        category: "Customisable",
      },
    },
    LinkComponent: {
      control: false, // Disable control for LinkComponent as it's defined automatically by the passed component
      description:
        'Implementation specific component to be used as Link component for logo link, i.e. Next.js Router\'s "Link" component, or React Router\'s "Link" component',
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
    logoLinkPath: {
      description: "Route path string to where logo link should navigate to",
      table: {
        category: "Customisable",
      },
    },
    logoAlt: {
      description: "Image alt text for the application logo (accessibility)",
      table: {
        category: "Customisable",
      },
    },
    title: {
      control: { type: "select" },
      description: "Fallback/secondary option for displaying a logo component",
      options: ["None", "MockTitleComponent"],
      mapping: {
        None: undefined,
        MockTitleComponent: (
          <Title order={4} mt={3}>
            Lasernet.components
          </Title>
        ),
      },
      table: {
        type: { summary: "ReactNode" },
        category: "Customisable",
      },
    },
    rightSection: {
      control: { type: "select" },
      description: "Header right section component to be rendered",
      options: ["None", "MockActions", "MockSignIn"],
      mapping: {
        None: undefined,
        MockActions: MockHeaderActions,
        MockSignIn: MockHeaderSignIn,
      },
      table: {
        type: { summary: "ReactNode" },
        category: "Customisable",
      },
    },
    leftSection: {
      control: { type: "select" },
      description: "Header left section component to be rendered, to the right of logo & title",
      options: ["None", "MockBreadcrumbs"],
      mapping: {
        None: undefined,
        MockBreadcrumbs: <MockBreadcrumbs />,
      },
      table: {
        type: { summary: "ReactNode" },
        category: "Customisable",
      },
    },
  },
  decorators: [
    (Story) => (
      <Box h={65} w={"100%"} style={{ overflow: "hidden" }}>
        <MockRouterProvider activePath={"/"}>
          <Story />
        </MockRouterProvider>
      </Box>
    ),
  ],
} satisfies Meta<typeof Header<MockLinkProps>>

export default meta
type Story = StoryObj<typeof Header<MockLinkProps>>

/**
 * Default
 */
export const Default: Story = {
  args: {
    logoLightModeSrc: "PortalLightModeLogo",
    logoDarkModeSrc: "DMDarkModeLogo",
    leftSection: "MockBreadcrumbs",
    rightSection: MockHeaderActions,
    title: "MockTitleComponent",
    logoAlt: "Lasernet.online Logo",
  },
}

/**
 * With Right Section
 */
export const WithRightSection: Story = {
  args: {
    logoLightModeSrc: "DMLightModeLogo",
    logoDarkModeSrc: "DMDarkModeLogo",
    leftSection: "None",
    rightSection: MockHeaderActions,
    title: "None",
    logoAlt: "Keep Logo",
  },
}

/**
 * With Left Section
 */
export const WithLeftSection: Story = {
  args: {
    logoLightModeSrc: "DMLightModeLogo",
    logoDarkModeSrc: "DMDarkModeLogo",
    leftSection: "MockBreadcrumbs",
    title: "None",
    logoAlt: "Keep Logo",
  },
}

/**
 * Fallback Title Component
 */
export const FallbackTitleComponent: Story = {
  args: {
    logoLightModeSrc: "None",
    logoDarkModeSrc: "None",
    leftSection: "MockBreadcrumbs",
    rightSection: MockHeaderActions,
    title: "MockTitleComponent",
    logoAlt: "Fallback Title Logo",
  },
}
