import type { Preview } from "@storybook/react"
import { useMantineColorScheme } from "@mantine/core"
import { DARK_MODE_EVENT_NAME, useDarkMode } from "storybook-dark-mode"
import { FC, useEffect, useState } from "react"
import { themes } from "@storybook/theming"
// Not sure why this doesn't respect the baseUrl in tsconfig.json
import { AppMantineProvider } from "components/AppMantineProvider/AppMantineProvider"
import { addons } from "@storybook/preview-api"
import { DocsContainer, DocsContainerProps } from "@storybook/blocks"
import { INITIAL_VIEWPORTS } from "@storybook/addon-viewport"

/**
 * Storybook rendering configuration file.
 *
 * Can be used to change the way stories are ordered, or provide context and additional data.
 */

/**
 * Switches the color scheme of the Mantine theme based on the dark mode state.
 * Only used in Storybook, and does not affect docs / .mdx files.
 */
const DarkModeSwitcher: FC = () => {
  // Get the setColorScheme function from the Mantine color scheme hook
  const { setColorScheme } = useMantineColorScheme()
  // Read the dark mode state from the Storybook dark mode addon
  const darkMode = useDarkMode()
  // Set the color scheme based on the dark mode state
  useEffect(() => {
    setColorScheme(darkMode ? "dark" : "light")
  }, [darkMode])
  // Return nothing as this component is only used for side effects
  return null
}

const channel = addons.getChannel()

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        date: /Date$/i,
      },
    },
    viewport: {
      viewports: {
        iphonex: INITIAL_VIEWPORTS.iphonex,
        iphonexr: INITIAL_VIEWPORTS.iphonexr,
        pixel: INITIAL_VIEWPORTS.pixel,
        pixelxl: INITIAL_VIEWPORTS.pixelxl,
        ipad: INITIAL_VIEWPORTS.ipad,
        smallLaptop: {
          name: "Small Laptop",
          styles: {
            width: "1366px",
            height: "768px",
          },
        },
        mediumLaptop: {
          name: "Medium Laptop",
          styles: {
            width: "1600px",
            height: "900px",
          },
        },
        largeLaptop: {
          name: "Large Laptop",
          styles: {
            width: "1920px",
            height: "1080px",
          },
        },
      },
    },
    backgrounds: {
      values: [
        { name: "Light Mode", value: "#f8f9fa" },
        { name: "Dark Mode", value: "#1f1f1f" },
        { name: "Mauve", value: "#f0edf5" },
        { name: "White", value: "#fff" },
        { name: "Black", value: "#000" },
      ],
    },
    darkMode: {
      // Override the default dark theme
      dark: { ...themes.dark, appBg: "#341d50" },
      // Override the default light theme
      light: { ...themes.normal, appBg: "#f4ecf7" },
    },
    docs: {
      // Enable dark mode on the documentation page
      container: (props: DocsContainerProps) => {
        const [isDark, setDark] = useState(false)
        useEffect(() => {
          channel.on(DARK_MODE_EVENT_NAME, setDark)
          return () => channel.removeListener(DARK_MODE_EVENT_NAME, setDark)
        }, [channel, setDark])
        return <DocsContainer {...props} theme={isDark ? themes.dark : themes.light} />
      },
      // Enable docs table of contents
      toc: {
        enable: true,
        title: "Contents",
      },
    },
  },
  /**
   *  Wrap all stories with the Tradernet mantine theme provider and a dark mode switcher.
   */
  decorators: [
    (Story) => {
      return (
        <AppMantineProvider>
          <DarkModeSwitcher />
          <Story />
        </AppMantineProvider>
      )
    },
  ],
  /**
   * Enable auto-generated documentation
   */
  tags: ["autodocs"],
}

export default preview
