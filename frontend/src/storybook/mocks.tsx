import { Context, createContext, FC, PropsWithChildren, useContext, useState, MouseEvent, forwardRef, ForwardRefExoticComponent, ReactNode } from "react"
import { Anchor, Breadcrumbs, Text } from "@mantine/core"
import ColorSchemeToggle from "components/layout/ColorSchemeToggle/ColorSchemeToggle"
import AccountDrawer from "components/layout/AccountDrawer/AccountDrawer"
import Button from "components/Button/Button"
import { SidebarItem } from "global/types"

/**
 * Mock router context for simulating updates to the active path on mock link clicks
 */
export const MockRouterContext: Context<{
  activePath: string
  navigate?: (path: string) => void
}> = createContext({ activePath: "/" })
export const useMockRouter = () => useContext(MockRouterContext)
export const MockRouterProvider: FC<{ activePath: string } & PropsWithChildren> = ({ activePath, children }) => {
  const [path, setPath] = useState(activePath)
  const navigate = (path: string) => setPath(path)
  return <MockRouterContext.Provider value={{ activePath: path, navigate }}>{children}</MockRouterContext.Provider>
}

/**
 * Mock link component
 */
export const mockLinkPropName = "href"
export type MockLinkProps = {
  href: string
} & PropsWithChildren &
  React.AnchorHTMLAttributes<HTMLAnchorElement>
export const MockLink: ForwardRefExoticComponent<MockLinkProps> = forwardRef<HTMLAnchorElement, MockLinkProps>(({ href, children, ...rest }, ref) => {
  const { navigate } = useMockRouter()
  const handleClick = (e: MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault()
    href?.length && navigate?.(href)
  }
  return (
    <a ref={ref} onClick={handleClick} href={href} {...rest}>
      {children}
    </a>
  )
})

/**
 * Mock action icon buttons for App Layout header
 */
export const MockHeaderActions: ReactNode[] = [<ColorSchemeToggle />, <AccountDrawer />]

/** Mock sign in button for App Layout header */
export const MockHeaderSignIn: ReactNode[] = [<Button leftIcon={"sign-in"}>Sign In</Button>]

/** Mock breadcrumb items */
const items = [
  { title: "Library", href: "#" },
  { title: "Components", href: "#" },
  { title: "Header", href: "#" },
].map((item, index) => (
  <Anchor href={item.href} key={index} c={"light-dark(var(--mantine-color-dark-4), var(--mantine-color-gray-3))"}>
    <Text size={"xs"} fw={600}>
      {item.title}
    </Text>
  </Anchor>
))

/** Mock breadcrumbs component */
export const MockBreadcrumbs: FC = () => (
  <Breadcrumbs separator={"→"} separatorMargin={"md"} mt={5}>
    {items}
  </Breadcrumbs>
)

/** mock bottom navigation link */
export const mockBottomLinkItem: SidebarItem = {
  label: "System Settings",
  icon: "gears",
  path: "/system-settings",
}
