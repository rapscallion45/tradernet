import { FC, Suspense } from "react"
import { Link as ReactRouterLink, LinkProps as ReactRouterLinkProps, Outlet, useLocation } from "react-router-dom"
import { Group, Image } from "@mantine/core"
import { IconHome, IconShield, IconUser, IconUsersGroup } from "@tabler/icons-react"
import Routes from "global/Routes"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import QuickNavigation from "components/QuickNavigation"
import AccountDrawer from "components/layout/AccountDrawer/AccountDrawer"
import { Title } from "components/Title/Title"
import AppLayout from "components/layout/AppLayout/AppLayout"
import Header from "components/layout/Header/Header"
import ColorSchemeToggle from "components/layout/ColorSchemeToggle/ColorSchemeToggle"
import Sidebar from "components/layout/Sidebar/Sidebar"
import useCurrentUser from "hooks/useCurrentUser"
import { SidebarItem } from "global/types"
import TradernetLogo from "assets/tradernet-logo.svg"

/** Define sidebar navigation menu items */
export const sidebarItems: SidebarItem[] = [
  {
    label: "Home",
    path: Routes.Dashboard,
    icon: <IconHome />,
  },
  {
    label: "Users & Groups",
    icon: <IconUsersGroup />,
    subItems: [
      {
        label: "Users",
        path: Routes.AdminUsers,
        icon: <IconUser />,
      },
      {
        label: "Groups",
        path: Routes.AdminGroups,
        icon: <IconUsersGroup />,
      },
    ],
  },
  {
    label: "Security Roles",
    path: Routes.AdminSecurityRoles,
    icon: <IconShield />,
  },
]

/**
 * Application base layout
 */
const Layout: FC = () => {
  const location = useLocation()
  const { data: currentUser } = useCurrentUser()
  const isSuperUser = (currentUser.roleNames ?? []).some((role) => role === "ALL Rights")
  const visibleSidebarItems = isSuperUser ? sidebarItems : sidebarItems.filter((item) => item.label !== "Security Roles")

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
      sidebar={
        <Sidebar<ReactRouterLinkProps>
          items={visibleSidebarItems}
          activePath={location.pathname}
          LinkComponent={ReactRouterLink}
          linkPropName={"to"}
        />
      }>
      <Suspense fallback={<PageLoadingSkeleton />}>
        <QuickNavigation />
        <Outlet />
      </Suspense>
    </AppLayout>
  )
}
export default Layout
