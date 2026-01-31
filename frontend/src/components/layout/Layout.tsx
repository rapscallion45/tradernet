import { FC, Suspense } from "react"
import { Link as ReactRouterLink, LinkProps as ReactRouterLinkProps, Outlet, useLocation } from "react-router-dom"
import { Title } from "@mantine/core"
import { IconHome } from "@tabler/icons-react"
import Routes from "global/Routes"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import AccountDrawer from "components/layout/AccountDrawer/AccountDrawer"
import AppLayout from "components/layout/AppLayout/AppLayout"
import Header from "components/layout/Header/Header"
import ColorSchemeToggle from "components/layout/ColorSchemeToggle/ColorSchemeToggle"
import Sidebar from "components/layout/Sidebar/Sidebar"
import { SidebarItem } from "global/types"

/** Define sidebar navigation menu items */
export const sidebarItems: SidebarItem[] = [
  {
    label: "Home",
    path: Routes.Dashboard,
    icon: <IconHome />,
  },
]

/**
 * Application base layout
 */
const Layout: FC = () => {
  const location = useLocation()

  return (
    <AppLayout
      header={
        <Header<ReactRouterLinkProps>
          title={<Title order={3}>Tradernet</Title>}
          LinkComponent={ReactRouterLink}
          linkPropName={"to"}
          logoLinkPath={"/"}
          rightSection={[<ColorSchemeToggle />, <AccountDrawer />]}
        />
      }
      sidebar={<Sidebar<ReactRouterLinkProps> items={sidebarItems} activePath={location.pathname} LinkComponent={ReactRouterLink} linkPropName={"to"} />}>
      <Suspense fallback={<PageLoadingSkeleton />}>
        <Outlet />
      </Suspense>
    </AppLayout>
  )
}
export default Layout
