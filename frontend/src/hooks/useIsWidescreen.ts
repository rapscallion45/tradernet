import { useMediaQuery } from "@mantine/hooks"
import { useMantineTheme } from "@mantine/core"

/**
 * Hook to determine if the current screen is beyond the maximum breakpoint
 */
export const useIsWidescreen = () => {
  const theme = useMantineTheme()
  return useMediaQuery(`(min-width: ${theme.breakpoints.xxl})`)
}
