import { FC, useCallback } from "react"
import { Burger } from "@mantine/core"
import { useGlobalStore } from "hooks/useGlobalStore"

/**
 * Header Burger Button component used as part of the mobile Header for expanding/collapsing sidebar
 */
const HeaderBurgerButton: FC = () => {
  const sidebarExpanded = useGlobalStore((state) => state.sidebarExpanded)
  const setSidebarExpanded = useGlobalStore((state) => state.setSidebarExpanded)

  const toggleSidebar = useCallback(() => setSidebarExpanded(!sidebarExpanded), [sidebarExpanded, setSidebarExpanded])

  return <Burger opened={sidebarExpanded} onClick={toggleSidebar} size={"sm"} hiddenFrom={"sm"} />
}

export default HeaderBurgerButton
