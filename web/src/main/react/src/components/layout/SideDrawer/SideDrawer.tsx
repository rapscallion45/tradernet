import { FC, ReactNode } from "react"
import { Affix, Box, Divider, Group, Paper, Text, Transition, ActionIcon } from "@mantine/core"
import { IconPin, IconPinnedOff, IconX } from "@tabler/icons-react"
import { SideDrawerSection, SideDrawerLocation } from "global/types"
import { useIsMobile } from "hooks/useIsMobile"
import { useLockBodyScroll } from "hooks/useLockBodyScroll"
import { useSideDrawer } from "hooks/useSideDrawer"
import classes from "./SideDrawer.module.css"

type SideDrawerProps = {
  name: string
  location?: SideDrawerLocation
  title: string | ReactNode
  keepMounted?: boolean
  // We don't use SideDrawerSection as we always want to render a header because it includes the close button and title
  headerContent?: ReactNode
  subtleHeader?: boolean
  footer?: SideDrawerSection
  pinnable?: boolean
  children: ReactNode
  onClose?: () => void | undefined
}

/**
 * A side drawer component that can be used to display content in a side panel.
 *
 * @param name The name of the side drawer. This is used to register the drawer with the global store.
 * @param location The location of the side drawer. This is used to register the drawer with the global store.
 * @param title The title of the side drawer or a custom ReactNode. This is displayed in the header of the side drawer.
 * @param keepMounted Whether the side drawer should be kept mounted when closed.
 * @param headerContent The header content of the side drawer, usually a button of some kind.
 * @param subtleHeader Whether the header should be subtle (gray background, no borders).
 * @param footer The footer content of the side drawer, can be anything. Also takes a `subtle` prop to make the footer gray, with no borders.
 * @param pinnable Whether the side drawer should be pinnable. Defaults to false.
 * @param children The main content of the side drawer. A <Stack> component is recommended.
 * @param onClose An optional onClose function for conditional logic.
 */
const SideDrawer: FC<SideDrawerProps> = ({
  name,
  location = "right",
  title,
  keepMounted,
  headerContent,
  subtleHeader,
  footer,
  pinnable,
  children,
  onClose,
}) => {
  const isMobile = useIsMobile()

  // Register the drawer with the global store
  const { opened, close, pinned, pin, unpin } = useSideDrawer(name, location)

  // Helper to check if title is a string
  const isStringTitle = typeof title === "string"

  // prevent double scrollbars when the side drawer is open
  useLockBodyScroll(opened)

  const align = location === "header" ? "right" : location

  // z-index of the AppShell is 100, so we need to set the z-index of the side drawers to be higher than that
  const zIndex = location === "header" ? 112 : location === "left" ? 111 : 110
  return (
    <Affix position={{ top: 0, right: 0 }} zIndex={zIndex}>
      <Transition
        mounted={opened}
        transition={align === "right" ? "slide-left" : "slide-right"}
        duration={250}
        timingFunction={"ease"}
        keepMounted={keepMounted}>
        {(transitionStyles) => (
          <Paper className={classes.drawer} style={transitionStyles} mod={{ mobile: isMobile, align }} data-testid={`side-drawer-${location}`}>
            <Box className={classes.header} data-testid={"side-drawer-header"} mod={{ subtle: subtleHeader }}>
              <Group justify={"space-between"} p={"md"} px={isMobile ? "md" : "lg"}>
                {isStringTitle ? (
                  <Text pt={5} fz={"md"} fw={700}>
                    {title}
                  </Text>
                ) : (
                  title
                )}
                <Group gap={"xs"}>
                  {headerContent}
                  {headerContent && <Divider orientation={"vertical"} mx={"sm"} />}
                  {pinnable && (
                    <ActionIcon size={"md"} variant={pinned ? "filled" : "subtle"} onClick={() => (pinned ? unpin() : pin())} aria-label={"Pin"}>
                      {pinned ? <IconPinnedOff /> : <IconPin />}
                    </ActionIcon>
                  )}
                  <ActionIcon variant={"subtle"} disabled={pinned} size={"md"} onClick={() => (onClose ? onClose() : close())} aria-label={"Close"}>
                    <IconX />
                  </ActionIcon>
                </Group>
              </Group>
            </Box>
            <Box className={classes.body} h={"100%"} p={isMobile ? "md" : "lg"} data-testid={"side-drawer-body"}>
              {children}
            </Box>
            {footer && (
              <Box p={"md"} px={isMobile ? "md" : "lg"} className={classes.footer} data-testid={"side-drawer-footer"} mod={{ subtle: footer?.subtle }}>
                {footer?.content}
              </Box>
            )}
          </Paper>
        )}
      </Transition>
    </Affix>
  )
}

export default SideDrawer
