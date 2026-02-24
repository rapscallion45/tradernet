import React, { FC, ReactElement, ReactNode, useEffect, useRef, useState } from "react"
import { Affix, Box, Card, Divider, Flex, Group, Menu, Space, Stack, Text, TitleProps, Transition } from "@mantine/core"
import { useWindowScroll } from "@mantine/hooks"
import { useIsMobile } from "../../hooks/useIsMobile"
import { Title } from "../Title/Title"
import { useGlobalStore } from "../../hooks/useGlobalStore"
import { ActionIcon } from "../ActionIcon/ActionIcon"
import { wrapNodeList } from "utils/nodes"
import { MenuItem } from "global/types"

export type PageHeaderProps = {
  // The title of the page, which must be a Tradernet Title component
  title: ReactElement<TitleProps, typeof Title>
  description?: string
  leftSection?: ReactNode[]
  rightSection?: ReactNode[]
  secondaryActions?: MenuItem[]
}

/**
 * PageHeader is a component that simplifies the creation of a page header, taking care of the layout and styling.
 * It is composed of a title, an optional description, and two optional sections
 * on the left and right which are used for actions and buttons.
 *
 * On mobile viewports the right section is affixed to the bottom of the page and is only visible when the user
 * scrolls to the top of the page, or scrolls back up.
 */
export const PageHeader: FC<PageHeaderProps> = (props) => {
  const isMobile = useIsMobile()

  const newProps: PageHeaderProps = {
    ...props,
    leftSection: wrapNodeList(props.leftSection),
    rightSection: wrapNodeList(props.rightSection),
  }

  return isMobile ? <MobileLayout {...newProps} /> : <DesktopLayout {...newProps} />
}

const MobileLayout: FC<PageHeaderProps> = ({ title, description, leftSection, rightSection, secondaryActions }) => {
  const [scroll] = useWindowScroll()
  const { sidebarExpanded, activeHeaderPanel, activeLeftPanel, activeRightPanel } = useGlobalStore()
  const atTopOfPage = scroll.y === 0
  const prevScrollY = useRef(scroll.y)
  const [isScrollingUp, setIsScrollingUp] = useState(false)

  useEffect(() => {
    if (scroll.y < prevScrollY.current) {
      setIsScrollingUp(true)
    } else {
      setIsScrollingUp(false)
    }
    prevScrollY.current = scroll.y
  }, [scroll.y])

  const isAnyPanelActive = !!activeLeftPanel || !!activeRightPanel || !!activeHeaderPanel

  const mounted = (atTopOfPage || isScrollingUp) && !isAnyPanelActive && !sidebarExpanded

  return (
    <Stack gap={"xs"}>
      <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
        {title}
        {secondaryActions && <SecondaryActionsMenu items={secondaryActions} mobile={true} />}
      </Group>

      {description && <Text>{description}</Text>}

      {leftSection && <Stack>{leftSection}</Stack>}
      <Affix position={{ bottom: 0, right: 0 }} w={"100%"}>
        <Transition transition="slide-up" mounted={mounted && !!rightSection} keepMounted>
          {(transitionStyles) => (
            <Box style={transitionStyles}>
              <Card w={"100%"} radius={0}>
                <Flex gap={"sm"} direction={"column-reverse"}>
                  {rightSection}
                </Flex>
              </Card>
            </Box>
          )}
        </Transition>
      </Affix>
    </Stack>
  )
}

const DesktopLayout: FC<PageHeaderProps> = ({ title, description, leftSection, rightSection, secondaryActions }) => {
  return (
    <Stack>
      <Group justify={"space-between"} align={"flex-start"} wrap={"nowrap"}>
        <Flex direction={"row"} gap={"sm"}>
          {title}
          {leftSection && (
            <Group justify={"stretch"} wrap={"nowrap"}>
              <Divider orientation="vertical" />
              {leftSection}
            </Group>
          )}
        </Flex>

        <Group wrap={"nowrap"}>
          {rightSection}
          {secondaryActions && <SecondaryActionsMenu items={secondaryActions} mobile={false} />}
        </Group>
      </Group>
      {description && <Text>{description}</Text>}
    </Stack>
  )
}

const SecondaryActionsMenu: FC<{ items: MenuItem[]; mobile: boolean }> = ({ items, mobile }) => (
  <Menu shadow={"md"} position={"bottom-end"}>
    <Menu.Target>
      <ActionIcon variant={"subtle"} size={mobile ? "xs" : "md"} matchFormSize icon={"ellipsis-vertical"} />
    </Menu.Target>
    <Menu.Dropdown>
      {items.map((item, index) => (
        <Menu.Item
          miw={150}
          key={index}
          leftSection={
            <Group w={16} align={"center"} justify={"center"}>
              {item.icon ? item.icon : <Space w={12} />}
            </Group>
          }
          onClick={(event) => {
            event.stopPropagation()
            item.onClick()
          }}
          p={"sm"}>
          <Text size={"sm"} px={"sm"}>
            {item.label}
          </Text>
        </Menu.Item>
      ))}
    </Menu.Dropdown>
  </Menu>
)
