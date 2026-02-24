import React, { FC, ReactNode, useCallback, useEffect, useMemo, useState } from "react"
import { Affix, Box, Divider, Group, Paper, Stack, ThemeIcon, Tooltip, Transition, UnstyledButton } from "@mantine/core"
import classes from "./SideDrawerTabbed.module.css"
import { useIsTablet } from "../../hooks/useIsTablet"
import { useIsWidescreen } from "../../hooks/useIsWidescreen"
import { SideDrawerLocation, SideDrawerSection } from "global/types"
import { useIsMobile } from "../../hooks/useIsMobile"
import { useLockBodyScroll } from "../../hooks/useLockBodyScroll"
import { ActionIcon } from "../ActionIcon/ActionIcon"
import { useSideDrawer } from "../../hooks/useSideDrawer"
import { IconArrowBigLeftLine } from "@tabler/icons-react"

export type SideDrawerTab = {
  id: string
  title: string
  content: ReactNode
  footer?: SideDrawerSection
}

export type SideDrawerTabbedProps = {
  name: string
  location: SideDrawerLocation
  tabs: SideDrawerTab[]
  expandable?: boolean
  pinnable?: boolean
  keepMounted?: boolean
  onTabChange?: (changeTabCallback: () => void) => void
  onClose?: (closeCallback: () => void) => void
  mainTabControl?: ReactNode
}

/**
 * A side drawer component that can be used to display content in a panel, with an expandable content area.
 *
 * The content is always shown on the main panel, but the first tab can be shifted across to the secondary panel as required.
 *
 * @param name The name of the side drawer. This is used to register the drawer with the global store.
 * @param location The location of the side drawer. This is used to register the drawer with the global store.
 * @param tabs The tabs to display in the side drawer.
 * @param expandable Whether the side drawer should be expandable. Defaults to false.
 * @param pinnable Whether the side drawer should be pinnable. Defaults to false.
 * @param keepMounted Whether the side drawer should be kept mounted when closed.
 * @param mainTabControl Optional additional content to show only on the 'main' visible tab (it doesn't show on the expanded tab). Appears above the tab contents.
 * @param onTabChange Optional callback for when a tab change is requested. If provided, the callback for actioning the tab change is passed as a prop, so must be called `onTabChange:(changeTab) => if(condition) changeTab()`
 * @param onClose Optional callback for when the side drawer close action is requested. If provided, the callback for actioning the close is passed as a prop, so must be called `onClose:(closeMe) => if(condition) closeMe()`
 */
export const SideDrawerTabbed: FC<SideDrawerTabbedProps> = ({
  name,
  location,
  tabs,
  expandable,
  pinnable,
  keepMounted,
  mainTabControl,
  onTabChange,
  onClose,
}) => {
  const isMobile = useIsMobile()
  const isTablet = useIsTablet()
  const isWide = useIsWidescreen()

  // Register the drawer with the global store
  const { opened, close, pinned, pin, unpin } = useSideDrawer(name, location)

  const isButtonVisible = useMemo(() => expandable && !(isMobile || isTablet), [expandable, isMobile, isTablet])

  // Close the drawer and unpin if the screen size changes to mobile or tablet
  useEffect(() => {
    if (isMobile || isTablet) {
      setExpanded(false)
      setButtonExpanded(false)
      unpin()
    }
  }, [isMobile, isTablet])

  // prevent double scrollbars when the side drawer is open
  useLockBodyScroll(opened)

  const [activeTab, setActiveTab] = useState(tabs[0].id)
  const [expanded, setExpanded] = useState(false)

  // Some extra state to manage the button animations:
  const [buttonExpanded, setButtonExpanded] = useState(false) // Used to add a slight delay to the expand animation
  const [buttonReady, setButtonReady] = useState(false) // Used to ensure the button slides out when the drawer is opened

  const changeTab = useCallback(
    (newTab: string) => {
      const callback = () => setActiveTab(newTab)
      if (onTabChange) onTabChange(callback)
      else callback()
    },
    [setActiveTab, onTabChange],
  )

  useEffect(() => {
    if (opened) {
      // Force a second render so the button CSS animation runs correctly on load
      setTimeout(() => {
        setButtonReady(true)
      }, 0)
    } else {
      setButtonReady(false)
    }
  }, [opened])

  const expand = () => {
    setExpanded((current) => {
      const next = !current

      // Set button move slightly delayed, for a slicker feel
      setTimeout(() => {
        setButtonExpanded(next)
      }, 50)

      return next
    })

    // On expand, if the first tab is currently selected, select the second tab
    if (!expanded && activeTab === tabs[0].id) setActiveTab(tabs[1].id)
    // On collapse, if the first tab is not selected, select it
    if (expanded && activeTab !== tabs[0].id) setActiveTab(tabs[0].id)
  }

  const animationProps = { transition: "slide-left", duration: 250, timingFunction: "ease" } as const

  // Here we manually control the floating button position, as it needs to live outside both panels
  const panelWidth = isWide ? 800 : 600 // panels are wider on HD screens
  const offset = 18 // button offset is always the same
  const buttonRight = panelWidth * (buttonExpanded ? 2 : 1) - offset

  // Sometimes we slice off the first tab
  const currentTabs = useMemo(() => tabs.slice(expanded ? 1 : 0), [tabs, expanded])
  const firstTab = useMemo(() => tabs[0], [tabs])

  return (
    <>
      {/* Main Panel. z-index is always 110 as the AppShell defaults to 100 / 101 */}
      <Affix zIndex={110}>
        <Transition mounted={opened} {...animationProps} keepMounted={keepMounted}>
          {(transitionStyles) => {
            const visibleTab = currentTabs.find((tab) => tab.id === activeTab)
            return (
              <>
                <Paper className={classes.mainDrawer} style={transitionStyles} data-testid={`side-drawer-${location}-primary`} mod={{ expanded }}>
                  <Box className={classes.header} data-testid={"side-drawer-header"}>
                    <Group justify={"space-between"} px={isMobile ? "sm" : "md"} wrap={"nowrap"} gap="xs">
                      <Group gap={0} className={classes.tabList} data-testid={`side-drawer-${location}-tabs`}>
                        {currentTabs.map((tab) => (
                          <UnstyledButton
                            key={tab.id}
                            onClick={() => changeTab(tab.id)}
                            className={classes.tab}
                            mod={{ selected: activeTab === tab.id }}
                            aria-label={`${tab.title} Tab`}>
                            {tab.title}
                          </UnstyledButton>
                        ))}
                      </Group>
                      {pinnable && !isMobile && (
                        <ActionIcon
                          tooltip={pinned ? "Unpin this panel" : "Pin this panel"}
                          icon={pinned ? "thumbtack-slash" : "thumbtack"}
                          size={"md"}
                          variant={pinned ? "filled" : "subtle"}
                          onClick={() => (pinned ? unpin() : pin())}
                          aria-label={"Pin"}
                        />
                      )}
                      <ActionIcon
                        disabled={pinned}
                        variant={"subtle"}
                        icon={"xmark"}
                        size={"md"}
                        onClick={onClose ? () => onClose(close) : close}
                        aria-label={"Close"}
                      />
                    </Group>
                  </Box>
                  {mainTabControl && (
                    <Stack gap={0}>
                      <Box>{mainTabControl}</Box>
                      <Divider m={0} />
                    </Stack>
                  )}
                  <Box className={classes.body} h={"100%"} p={isMobile ? "md" : "lg"} data-testid={"side-drawer-body"}>
                    {visibleTab?.content}
                  </Box>
                  {visibleTab?.footer && (
                    <Box
                      p={"md"}
                      px={isMobile ? "md" : "lg"}
                      className={classes.footer}
                      data-testid={"side-drawer-footer"}
                      mod={{ subtle: visibleTab.footer.subtle }}>
                      {visibleTab.footer?.content}
                    </Box>
                  )}
                </Paper>
                {/* Floating Button */}
                <ThemeIcon
                  style={{
                    visibility: isButtonVisible ? "visible" : "hidden",
                    // position based on readiness and panel expansion
                    right: !opened ? 0 : !buttonReady ? 0 : buttonRight,
                    // fade out as we close the panel
                    opacity: opened && buttonReady ? 1 : 0,
                    // nice animation that slides and rotates the button
                    transition: "right 250ms ease, transform 300ms ease, opacity 250ms ease",
                    transform: buttonExpanded ? "rotate(-180deg)" : "rotate(0deg)",
                  }}
                  radius="xl"
                  size="lg"
                  onClick={expand}
                  className={classes.floatingButton}
                  mod={{ expanded }}>
                  <Tooltip label={"Expand Panel"}>
                    <IconArrowBigLeftLine aria-label={"Expand Panel"} />
                  </Tooltip>
                </ThemeIcon>
              </>
            )
          }}
        </Transition>
      </Affix>
      {/* Secondary Panel */}
      <Affix zIndex={9}>
        <Transition mounted={opened && expanded} {...animationProps} keepMounted={keepMounted}>
          {(transitionStyles) => {
            // This panel only ever shows the first tab in the list. Could be made more flexible later if desired, but K.I.S.S for now.
            return (
              <Paper className={classes.secondaryDrawer} style={transitionStyles} data-testid={`side-drawer-${location}-secondary`}>
                <Box className={classes.header} data-testid={"side-drawer-header"}>
                  <Group justify={"space-between"} px={isMobile ? "sm" : "md"}>
                    <Group gap={0} pt={"md"}>
                      <UnstyledButton disabled key={firstTab.id} className={classes.tab} mod={{ selected: true }}>
                        {firstTab.title}
                      </UnstyledButton>
                    </Group>
                  </Group>
                </Box>
                <Box className={classes.body} h={"100%"} p={isMobile ? "md" : "lg"} data-testid={"side-drawer-body"}>
                  {firstTab.content}
                </Box>
                {firstTab.footer && (
                  <Box
                    p={"md"}
                    px={isMobile ? "md" : "lg"}
                    className={classes.footer}
                    data-testid={"side-drawer-footer"}
                    mod={{ subtle: firstTab.footer.subtle }}>
                    {firstTab.footer?.content}
                  </Box>
                )}
              </Paper>
            )
          }}
        </Transition>
      </Affix>
    </>
  )
}
