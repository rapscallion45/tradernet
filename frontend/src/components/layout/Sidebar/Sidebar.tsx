import { ComponentType, Fragment, useEffect, useState } from "react"
import { ActionIcon, Collapse, Divider, Flex, Group, List, ListItem, Popover, ScrollArea, Stack, Text, Tooltip } from "@mantine/core"
import { IconArrowBarLeft, IconArrowBarRight } from "@tabler/icons-react"
import { useGlobalStore } from "hooks/useGlobalStore"
import { useDesktopSidebarExpandedStorage } from "hooks/useDesktopSidebarExpandedStorage"
import { useLockBodyScroll } from "hooks/useLockBodyScroll"
import { useIsMobile } from "hooks/useIsMobile"
import { SidebarItem } from "global/types"
import classes from "./Sidebar.module.css"

/**
 * Sidebar props
 * @prop items - List of menu and submenu items (nav links) to be rendered
 * @prop activePath - Currently active path used for active link indication styling
 * @prop LinkComponent - Generic link component type used, i.e. Next.js' "Link", React Router's "Link", etc
 * @prop linkPropName - Name of the prop that is used as the pathname for passed LinkComponent, i.e. "href", "to", etc
 * @prop bottomLinkItem - Optional link passed to enable custom button to be displayed at bottom of nav bar, e.g. link to a "System Settings" page
 */
export type SidebarProps<T> = {
  items: SidebarItem[]
  activePath: string
  LinkComponent: ComponentType<T>
  linkPropName: keyof T
  bottomLinkItem?: SidebarItem
}

/**
 * Generic Sidebar component used for displaying side navigation links. Sidebar can either be
 * collapsed or expanded, with sub menu behaviour changing between popover menu or sub menu list, respectively.
 *
 * Props are a set of custom arguments to enable the Sidebar to be used with any routing library. The sidebar
 * must be typed to coincide with the router "Link" component used (e.g. Next.js' "Link"), along with
 * being passed the link component itself and the name of the "path" prop of the passed LinkComponent (i.e. React
 * Router's Link uses "to", whereas Next.js' "Link" component uses "href").
 *
 * The nav items are passed as an array of type "FormpipeSidebarItem", and include all information for each sidebar
 * link to be rendered, including any sub menu links (*currently limited to only one level of sub menu items*). The
 * active path string is also passed, allowing for the currently active link to be styled accordingly.
 *
 * Two expansion mechanisms are used, one for desktop, that saves the users' expansion preference in local storage,
 * and one for mobile, that uses the global store hook to control the expansion state. By design, the sidebar will
 * automatically close on mobile when a link is clicked, whereas desktop will remain in its preference state.
 *
 * An optional bottom navigation link item is available for positioning a link at the bottom of the sidebar close to
 * the expand/collapse toggle button. It is intended for this to be used for things like "System Settings",
 * "Preferences", etc. This is of type "FormpipeSidebarItem", but this feature does not support sub menu items. Any
 * sub menu items that are provided will be ignored.
 */
const Sidebar = <T,>({ items, activePath, LinkComponent, linkPropName, bottomLinkItem }: SidebarProps<T>) => {
  const mobileSidebarExpanded = useGlobalStore((state) => state.sidebarExpanded)
  const setMobileSidebarExpanded = useGlobalStore((state) => state.setSidebarExpanded)
  const [desktopSidebarExpanded, setDesktopSidebarExpanded] = useDesktopSidebarExpandedStorage()
  const [popoverOpen, setPopoverOpen] = useState<string>()
  const isMobile = useIsMobile()
  useLockBodyScroll(false)

  /**
   * if there is a change in the active path, i.e. user has clicked a link, or device type, close
   * the sidebar on mobile
   */
  useEffect(() => setMobileSidebarExpanded(false), [activePath, isMobile, setMobileSidebarExpanded])

  return (
    <Flex w={mobileSidebarExpanded || desktopSidebarExpanded ? "100%" : 59} className={classes.root}>
      <List style={{ overflow: "hidden" }}>
        <ScrollArea h={"100%"} scrollbars={"y"} scrollbarSize={mobileSidebarExpanded || desktopSidebarExpanded ? 10 : 5}>
          {items.map((navItem: SidebarItem) => (
            <Fragment key={`${navItem.label}_${navItem.path}`}>
              <Popover
                opened={
                  desktopSidebarExpanded
                    ? popoverOpen === navItem.label &&
                      activePath !== navItem.path &&
                      !navItem.subItems?.some((item) => item.path === activePath || (item.path && activePath.includes(item.path)))
                    : popoverOpen === navItem.label
                }
                position={"right-start"}>
                {/** Popover targets for each menu list item */}
                <Tooltip
                  label={navItem.label}
                  position={"right"}
                  disabled={isMobile || desktopSidebarExpanded || Boolean(navItem.subItems?.length)}
                  ml={"-30px"}>
                  <Popover.Target>
                    <LinkComponent {...({ [linkPropName]: navItem.path ?? navItem.subItems?.[0].path } as T)} style={{ textDecoration: "none" }}>
                      <ListItem
                        icon={navItem.icon ? <Flex justify={"center"}>{navItem.icon}</Flex> : null}
                        onMouseEnter={() => setPopoverOpen(navItem.label)}
                        onMouseLeave={() => setPopoverOpen(undefined)}
                        classNames={{
                          item: classes.item,
                          itemLabel: classes.itemLabel,
                          itemIcon: classes.itemIcon,
                        }}
                        data-active={
                          activePath === navItem.path ||
                          (!desktopSidebarExpanded &&
                            !mobileSidebarExpanded &&
                            navItem.subItems?.some((item) => item.path === activePath || (item.path && activePath.includes(item.path))))
                        }
                        data-submenuheader={
                          (desktopSidebarExpanded || mobileSidebarExpanded) &&
                          navItem.subItems?.some((item) => item.path === activePath || (item.path && activePath.includes(item.path)))
                        }
                        data-submenuhovered={popoverOpen === navItem.label}
                        data-testid={`${navItem.label}-menu-item`}>
                        {desktopSidebarExpanded || mobileSidebarExpanded ? navItem.label : null}
                      </ListItem>
                    </LinkComponent>
                  </Popover.Target>
                </Tooltip>
                {/** Popover sub menu list items for respective menu item, only rendered on desktop */}
                {navItem.subItems?.length && (
                  <Popover.Dropdown
                    onMouseEnter={() => setPopoverOpen(navItem.label)}
                    onMouseLeave={() => setPopoverOpen(undefined)}
                    visibleFrom={"sm"}
                    p={"0 0 10px 0"}
                    ml={"-32px"}
                    data-testid={`${navItem.label}-popover-menu-dropdown`}>
                    <Text size={"sm"} fw={700} pt={10} px={20}>
                      {navItem.label}
                    </Text>
                    {navItem.subItems?.length && (
                      <List pt={5}>
                        {navItem.subItems?.map((subItem) => (
                          <LinkComponent key={subItem.path} {...({ [linkPropName]: subItem.path } as T)}>
                            <ListItem
                              icon={subItem.icon ? <Flex justify={"center"}>{subItem.icon}</Flex> : null}
                              onClick={() => setPopoverOpen(undefined)}
                              classNames={{
                                item: classes.popoverItem,
                                itemLabel: classes.subItemLabel,
                                itemIcon: classes.popoverItemIcon,
                              }}
                              data-active={subItem.path === activePath || (subItem.path && activePath.includes(subItem.path))}
                              data-testid={`${subItem.label}-popover-menu-item`}>
                              {subItem.label}
                            </ListItem>
                          </LinkComponent>
                        ))}
                      </List>
                    )}
                  </Popover.Dropdown>
                )}
              </Popover>
              {/** Submenu list items, only rendered when sidebar is expanded */}
              {(desktopSidebarExpanded || mobileSidebarExpanded) && navItem.subItems?.length && (
                <Collapse
                  in={
                    isMobile ||
                    activePath === navItem.path ||
                    navItem.subItems.some((item) => item.path === activePath || (item.path && activePath.includes(item.path)))
                  }
                  className={classes.subMenu}>
                  {navItem.subItems.map((subItem) => (
                    <LinkComponent key={subItem.path} {...({ [linkPropName]: subItem.path } as T)} style={{ textDecoration: "none" }}>
                      <ListItem
                        icon={subItem.icon ? <Flex justify={"center"}>{subItem.icon}</Flex> : null}
                        classNames={{
                          item: classes.subMenuItem,
                          itemLabel: classes.itemLabel,
                          itemIcon: classes.itemIcon,
                        }}
                        data-active={subItem.path === activePath || (subItem.path && activePath.includes(subItem.path))}
                        data-testid={`${subItem.label}-sub-menu-item`}>
                        {subItem.label}
                      </ListItem>
                    </LinkComponent>
                  ))}
                </Collapse>
              )}
            </Fragment>
          ))}
        </ScrollArea>
      </List>
      <Stack gap={0} mt={5}>
        <Divider mx={desktopSidebarExpanded || mobileSidebarExpanded ? 20 : 7} />
        <Group justify={bottomLinkItem ? "flex-start" : "flex-end"} gap={0}>
          {/** Optional bottom link item area */}
          {bottomLinkItem && (
            <Tooltip label={bottomLinkItem.label} position={"right"} disabled={isMobile || desktopSidebarExpanded} ml={"-15px"}>
              <LinkComponent {...({ [linkPropName]: bottomLinkItem.path } as T)} style={{ textDecoration: "none" }}>
                <List>
                  <ListItem
                    icon={bottomLinkItem.icon ? <Flex justify={"center"}>{bottomLinkItem.icon}</Flex> : null}
                    classNames={{
                      item: classes.item,
                      itemLabel: classes.itemLabel,
                      itemIcon: classes.itemIcon,
                    }}
                    w={mobileSidebarExpanded ? "100vw" : desktopSidebarExpanded ? "239px" : "65px"}
                    data-active={bottomLinkItem.path && activePath.includes(bottomLinkItem.path)}
                    data-testid={`${bottomLinkItem.label}-bottom-link-item`}>
                    {desktopSidebarExpanded || mobileSidebarExpanded ? bottomLinkItem.label : null}
                  </ListItem>
                </List>
              </LinkComponent>
            </Tooltip>
          )}
          {/** Expand/collapse toggle, only rendered on desktop */}
          <Flex justify={"flex-end"} py={10} px={13} visibleFrom={"sm"}>
            <Tooltip label={desktopSidebarExpanded ? "Collapse" : "Expand"} position={"right"} disabled={isMobile}>
              <ActionIcon onClick={() => setDesktopSidebarExpanded(!desktopSidebarExpanded)} variant={"subtle"}>
                {desktopSidebarExpanded ? <IconArrowBarLeft /> : <IconArrowBarRight />}
              </ActionIcon>
            </Tooltip>
          </Flex>
        </Group>
      </Stack>
    </Flex>
  )
}

export default Sidebar
