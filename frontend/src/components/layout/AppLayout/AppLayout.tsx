import { FC, PropsWithChildren, ReactNode } from "react"
import { AppShell, Box } from "@mantine/core"
import { useGlobalStore } from "hooks/useGlobalStore"
import { useDesktopSidebarExpandedStorage } from "hooks/useDesktopSidebarExpandedStorage"

/**
 * AppLayout props
 * @prop header - header component to be rendered, it is assumed that this will be required in any consuming application
 * @prop sidebar - sidebar component to be rendered, if not supplied AppLayout removes the sidebar area from the layout
 * @prop footer - footer component to be rendered, if not supplied AppLayout removes the footer area from the layout
 */
export type AppLayoutProps = {
  header: ReactNode
  sidebar?: ReactNode
  footer?: ReactNode
} & PropsWithChildren

/**
 * AppLayout component used for rendering the base layout of a Tradernet dashboard/UI.
 */
const AppLayout: FC<AppLayoutProps> = ({ header, sidebar, footer, children }) => {
  const mobileSidebarExpanded = useGlobalStore((state) => state.sidebarExpanded)
  const [desktopSidebarIsExpanded] = useDesktopSidebarExpandedStorage()

  return (
    <AppShell
      header={{ height: { base: 45, xs: 65 } }}
      navbar={{
        width: { base: "100%", sm: desktopSidebarIsExpanded ? 300 : 60 },
        breakpoint: "sm",
        collapsed: { mobile: !mobileSidebarExpanded, desktop: !sidebar },
      }}
      footer={{ height: 45, collapsed: !footer }}
      padding={{ base: "xs", sm: "md" }}>
      <AppShell.Header>{header}</AppShell.Header>
      {/** Desktop sidebar styling */}
      {sidebar && (
        <AppShell.Navbar visibleFrom={"sm"} style={{ borderRight: "solid 1px light-dark(var(--mantine-color-gray-3), var(--mantine-color-dark-4))" }}>
          {sidebar}
        </AppShell.Navbar>
      )}
      {/** Mobile sidebar styling */}
      {sidebar && (
        <AppShell.Navbar hiddenFrom={"sm"} style={{ border: "none" }}>
          {sidebar}
        </AppShell.Navbar>
      )}
      {footer && <AppShell.Footer>{footer}</AppShell.Footer>}
      <AppShell.Main>
        <Box pt={"md"} px={"md"}>
          {children}
        </Box>
      </AppShell.Main>
    </AppShell>
  )
}

export default AppLayout
