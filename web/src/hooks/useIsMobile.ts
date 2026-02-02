import { useMediaQuery } from "@mantine/hooks"
import { useMantineTheme } from "@mantine/core"

/**
 * Hook to determine if the current screen size is mobile.
 */
export const useIsMobile = () => {
  const theme = useMantineTheme()
  return useMediaQuery(`(max-width: ${theme.breakpoints.xs})`)
}
