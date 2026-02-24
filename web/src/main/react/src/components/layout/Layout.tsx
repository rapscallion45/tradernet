import { FC, Suspense } from "react"
import { Link as ReactRouterLink, LinkProps as ReactRouterLinkProps, Outlet, useLocation } from "react-router-dom"
import { Group, Image } from "@mantine/core"
import { IconHome } from "@tabler/icons-react"
import Routes from "global/Routes"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import QuickNavigation from "components/QuickNavigation"
import AccountDrawer from "components/layout/AccountDrawer/AccountDrawer"
import { Title } from "components/Title/Title"
import AppLayout from "components/layout/AppLayout/AppLayout"
import Header from "components/layout/Header/Header"
import ColorSchemeToggle from "components/layout/ColorSchemeToggle/ColorSchemeToggle"
import Sidebar from "components/layout/Sidebar/Sidebar"
import { SidebarItem } from "global/types"
import TradernetLogo from "assets/tradernet-logo.svg"

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
          title={<Title subtitle>Tradernet</Title>}
          LinkComponent={ReactRouterLink}
          linkPropName={"to"}
          logoLinkPath={"/"}
          leftSection={[
            <Group>
              <Image src={TradernetLogo} alt={"Tradernet logo"} h={40} w={"auto"} />
              <Title subtitle>Tradernet</Title>
            </Group>,
          ]}
          rightSection={[<ColorSchemeToggle />, <AccountDrawer />]}
        />
      }
      sidebar={<Sidebar<ReactRouterLinkProps> items={sidebarItems} activePath={location.pathname} LinkComponent={ReactRouterLink} linkPropName={"to"} />}>
      <Suspense fallback={<PageLoadingSkeleton />}>
        <QuickNavigation />
        <Outlet />
      </Suspense>
    </AppLayout>
  )
}
export default Layout
