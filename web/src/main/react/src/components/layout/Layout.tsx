import { FC, Suspense } from "react"
import { Link as ReactRouterLink, LinkProps as ReactRouterLinkProps, Outlet, useLocation } from "react-router-dom"
import { Group, Image, TextInput } from "@mantine/core"
import { spotlight } from "@mantine/spotlight"
import { IconHistory, IconHome, IconShield, IconUser, IconUsersGroup } from "@tabler/icons-react"
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
    label: "Order History",
    path: Routes.Orders,
    icon: <IconHistory />,
  },
  {
    label: "Users & Groups",
    icon: <IconUsersGroup />,
    subItems: [
      {
        label: "Users",
        path: Routes.AdminUsers,
        icon: <IconUser size={15} />,
      },
      {
        label: "Groups",
        path: Routes.AdminGroups,
        icon: <IconUsersGroup size={15} />,
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
  const roleNames = currentUser?.roleNames ?? []
  const canViewUserAndGroupNavigation = roleNames.some((role) => role === "ALL Rights" || role === "Admin Rights")
  const canViewSecurityRolesNavigation = roleNames.some((role) => role === "ALL Rights")

  const visibleSidebarItems = sidebarItems.filter((item) => {
    if (item.label === "Users & Groups") {
      return canViewUserAndGroupNavigation
    }

    if (item.label === "Security Roles") {
      return canViewSecurityRolesNavigation
    }

    return true
  })

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
          rightSection={[
            <TextInput
              placeholder={"Search (Ctrl + K)"}
              onClick={spotlight.open}
              onKeyDown={(e) => {
                e.preventDefault()
                spotlight.open()
              }}
              size={"md"}
              variant={"filled"}
              miw={150}
              aria-label={"Search"}
            />,
            <ColorSchemeToggle />,
            <AccountDrawer />,
          ]}
        />
      }
      sidebar={
        <Sidebar<ReactRouterLinkProps> items={visibleSidebarItems} activePath={location.pathname} LinkComponent={ReactRouterLink} linkPropName={"to"} />
      }>
      <Suspense fallback={<PageLoadingSkeleton />}>
        <QuickNavigation />
        <Outlet />
      </Suspense>
    </AppLayout>
  )
}
export default Layout
