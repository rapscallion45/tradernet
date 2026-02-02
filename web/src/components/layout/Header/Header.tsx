import { ComponentType, ReactNode } from "react"
import { Box, Group, Paper } from "@mantine/core"
import HeaderBurgerButton from "components/layout/Header/HeaderBurgerButton"
import HeaderLogos from "components/layout/Header/HeaderLogos"
import { useIsMobile } from "hooks/useIsMobile"
import { wrapNodeList } from "utils/nodes"

/**
 * Header props
 * @param LinkComponent - component to be used for logo link
 * @param linkPropName - "path" prop name of passed Link component (e.g. Next.js' Link uses "href")
 * @param logoLinkPath - route path that should be navigated to on logo click
 * @param logoLightModeSrc - application logo component to be displayed right of Tradernet "crest" logo in "light mode"
 * @param logoDarkModeSrc - application logo component to be displayed right of Tradernet "crest" logo in "dark mode"
 * @param title - a fallback component to display in place of application logo if not provided
 * @param rightSection - optional component to be rendered on the right hand side of the header
 * @param leftSection - optional component to be rendered on the left hand side of the header, right of logo & title
 * @param logoAlt - alt text for the application logo (accessibility)
 */
export type HeaderProps<T> = {
  LinkComponent: ComponentType<T>
  linkPropName: keyof T
  logoLinkPath: string
  logoLightModeSrc?: string
  logoDarkModeSrc?: string
  title?: ReactNode
  rightSection?: ReactNode[]
  leftSection?: ReactNode
  logoAlt?: string
}

/**
 * Header component that is used as part of the App Layout.
 *
 * Similar to Sidebar, a LinkComponent is passed to allow the header logo link to be used with any routing library. (See
 * Sidebar for details).
 *
 * It is expected that the consuming application will pass both light and dark mode logo sources. These sources *ideally*
 * should be .svg format - all sizing and positioning is then handled by the Header component. If either, or both, logo
 * sources are **not** used, a "title" component can be passed instead as a fallback. Logo sources will take
 * precedence if a combination of options are passed (i.e. a light logo source is passed, no dark logo source, and
 * a title component - this would result in the light logo source being rendered in light mode and the title
 * fallback component being rendered in dark mode).
 */
const Header = <T,>({
  logoLightModeSrc,
  logoDarkModeSrc,
  LinkComponent,
  linkPropName,
  logoLinkPath,
  title,
  rightSection,
  leftSection,
  logoAlt,
}: HeaderProps<T>) => {
  const isMobile = useIsMobile()

  const logoAriaLabel = logoAlt || (typeof title === "string" ? title : "Home")

  return (
    <Paper h={"100%"} bg={"light-dark(var(--mantine-color-white), var(--mantine-color-dark-9))"}>
      <Group h={"100%"} px={"md"} justify={"space-between"} wrap={"nowrap"}>
        <Group gap={5} h={"100%"} wrap={"nowrap"}>
          <HeaderBurgerButton />
          <LinkComponent
            {...({ [linkPropName]: logoLinkPath } as T)}
            style={{
              textDecoration: "none",
              color: "light-dark(var(--mantine-color-gray-9), var(--mantine-color-gray-1))",
            }}
            aria-label={logoAriaLabel}>
            <HeaderLogos logoLightModeSrc={logoLightModeSrc} logoDarkModeSrc={logoDarkModeSrc} title={title} logoAlt={logoAlt} />
          </LinkComponent>
          {leftSection && (
            <Box pl={20} visibleFrom={"md"}>
              {leftSection}
            </Box>
          )}
        </Group>
        {rightSection && <Group gap={isMobile ? "xs" : "md"}>{wrapNodeList(rightSection)}</Group>}
      </Group>
    </Paper>
  )
}

export default Header
