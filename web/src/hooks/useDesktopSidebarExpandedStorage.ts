import { useLocalStorage } from "@mantine/hooks"

/**
 * Hook to store Sidebar expanded/collapsed preference.
 */
export const useDesktopSidebarExpandedStorage = () => {
  return useLocalStorage<boolean>({
    key: "formpipe-sidebar-expanded",
    defaultValue: false,
  })
}
