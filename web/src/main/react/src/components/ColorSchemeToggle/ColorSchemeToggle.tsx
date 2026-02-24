import { FC } from "react"
import { useComputedColorScheme, useMantineColorScheme } from "@mantine/core"
import { ActionIcon } from "../ActionIcon/ActionIcon"

export const ColorSchemeToggle: FC = () => {
  const { setColorScheme } = useMantineColorScheme()
  const colorScheme = useComputedColorScheme()
  const isLight = colorScheme === "light"
  return (
    <ActionIcon
      tooltip={`Switch to ${isLight ? "Dark" : "Light"} Mode`}
      variant="subtle"
      aria-label="Color Scheme Toggle"
      icon={isLight ? "moon" : "sun-bright"}
      size={"lg"}
      onClick={() => setColorScheme(isLight ? "dark" : "light")}
    />
  )
}
