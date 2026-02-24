"use client"

import { FC } from "react"
import { Burger } from "@mantine/core"
import { useGlobalStore } from "../../hooks/useGlobalStore"

/**
 * Header Burger Button component used as part of the mobile Header for expanding/collapsing sidebar
 */
export const HeaderBurgerButton: FC = () => {
  const [sidebarExpanded, setSidebarExpanded] = useGlobalStore((state) => [state.sidebarExpanded, state.setSidebarExpanded])

  return <Burger opened={sidebarExpanded} onClick={() => setSidebarExpanded(!sidebarExpanded)} size="sm" hiddenFrom="md" />
}
