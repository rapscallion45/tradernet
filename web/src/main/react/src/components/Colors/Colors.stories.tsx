import { Stack, Switch, Text, Tooltip } from "@mantine/core"
import { ColorSwatch, Grid, Group, Title, useMantineTheme } from "@mantine/core"
import type { Meta } from "@storybook/react"
import { useClipboard } from "@mantine/hooks"
import { useState } from "react"

/**
 * A set of color swatches to display the colors available in the current theme.
 *
 * Clicking on a swatch will copy the hex value to the clipboard.
 */
const meta = {
  title: "Examples/Colors",
} satisfies Meta<typeof ColorSwatch>

export default meta

export const ColorTool = () => {
  const theme = useMantineTheme()
  // Allow users to copy the color value with or without the hash
  const [includeHash, setIncludeHash] = useState(true)
  // Use the useClipboard hook to copy the color value to the clipboard
  const clipboard = useClipboard({ timeout: 500 })

  const performCopy = (color: string) => {
    const valueToCopy = includeHash ? color : color.slice(1)
    console.log("Copying color", valueToCopy)
    clipboard.copy(valueToCopy)
  }

  return (
    <Stack>
      <Switch size={"md"} label={"Include # when copying"} checked={includeHash} onChange={(e) => setIncludeHash(e.currentTarget.checked)} />
      <Group>
        {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9].map((color, index) => (
          <ColorSwatch color={"white"} key={index} withShadow={true}>
            {index}
          </ColorSwatch>
        ))}
      </Group>
      {Object.entries(theme.colors).map(([colorName, colorValues], index) => (
        <div key={index}>
          <Text>{colorName.toUpperCase()}</Text>
          <Group>
            {colorValues.map((color, index) => (
              <Tooltip key={index} label={clipboard.copied ? "Copied!" : color} position="top">
                <ColorSwatch color={color} key={index} withShadow={true} onClick={() => performCopy(color)} />
              </Tooltip>
            ))}
          </Group>
        </div>
      ))}
    </Stack>
  )
}
