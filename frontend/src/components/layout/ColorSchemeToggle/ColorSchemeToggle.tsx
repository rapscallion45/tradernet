import { FC } from "react"
import { useComputedColorScheme, useMantineColorScheme, ActionIcon } from "@mantine/core"
import { IconMoon, IconSun } from "@tabler/icons-react"

/**
 * Application Color Scheme Toggle Button component
 * @constructor
 */
const ColorSchemeToggle: FC = () => {
  const { setColorScheme } = useMantineColorScheme()
  const colorScheme = useComputedColorScheme()
  const isLight = colorScheme === "light"
  return (
    <ActionIcon variant={"subtle"} aria-label={"Color Scheme Toggle"} size={"lg"} onClick={() => setColorScheme(isLight ? "dark" : "light")}>
      {isLight ? <IconMoon /> : <IconSun />}
    </ActionIcon>
  )
}

export default ColorSchemeToggle
