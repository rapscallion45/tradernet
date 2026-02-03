import { useMediaQuery } from "@mantine/hooks"
import { useMantineTheme } from "@mantine/core"

/**
 * Hook to determine if the current screen size is mobile.
 */
export const useIsTablet = () => {
  const theme = useMantineTheme()
  return useMediaQuery(`(min-width: ${theme.breakpoints.xs}) and (max-width: ${theme.breakpoints.lg})`)
}
