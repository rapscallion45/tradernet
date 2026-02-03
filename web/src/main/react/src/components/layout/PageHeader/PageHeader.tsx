import { FC, ReactElement, ReactNode, useEffect, useRef, useState } from "react"
import { Affix, Box, Card, Divider, Flex, Group, Popover, Stack, Text, TitleProps, Transition } from "@mantine/core"
import { useWindowScroll } from "@mantine/hooks"
import { useIsMobile } from "hooks/useIsMobile"
import { useGlobalStore } from "hooks/useGlobalStore"
import Title from "components/Title/Title"
import ActionIcon from "components/ActionIcon/ActionIcon"
import { wrapNodeList } from "utils/nodes"

type PageHeaderProps = {
  // The title of the page, which must be a Tradernet Title component
  title: ReactElement<TitleProps, typeof Title>
  description?: string
  leftSection?: ReactNode[]
  rightSection?: ReactNode[]
  hiddenSection?: ReactNode[]
}

/**
 * PageHeader is a component that simplifies the creation of a page header, taking care of the layout and styling.
 * It is composed of a title, an optional description, and two optional sections
 * on the left and right which are used for actions and buttons.
 *
 * On mobile viewports the right section is affixed to the bottom of the page and is only visible when the user
 * scrolls to the top of the page, or scrolls back up.
 */
const PageHeader: FC<PageHeaderProps> = (props) => {
  const isMobile = useIsMobile()

  const newProps: PageHeaderProps = {
    ...props,
    leftSection: wrapNodeList(props.leftSection),
    rightSection: wrapNodeList(props.rightSection),
    hiddenSection: wrapNodeList(props.hiddenSection),
  }

  return isMobile ? <MobileLayout {...newProps} /> : <DesktopLayout {...newProps} />
}

const MobileLayout: FC<PageHeaderProps> = ({ title, description, leftSection, rightSection, hiddenSection }) => {
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
      <Group justify={"space-between"} align={"flex-start"}>
        {title}
        {hiddenSection && (
          <Popover position={"bottom"} offset={0}>
            <Popover.Target>
              <ActionIcon variant={"subtle"} size={"xs"} matchFormSize icon={"ellipsis-vertical"} />
            </Popover.Target>
            <Popover.Dropdown p={"sm"} w={"calc(100% - 10px)"}>
              <Stack p={0} gap={"sm"}>
                {hiddenSection}
              </Stack>
            </Popover.Dropdown>
          </Popover>
        )}
      </Group>

      {description && <Text>{description}</Text>}

      {leftSection && <Stack>{leftSection}</Stack>}
      <Affix position={{ bottom: 0, right: 0 }} w={"100%"}>
        <Transition transition={"slide-up"} mounted={mounted && !!rightSection} keepMounted>
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

const DesktopLayout: FC<PageHeaderProps> = ({ title, description, leftSection, rightSection, hiddenSection }) => {
  return (
    <Stack>
      <Group justify={"space-between"} align={"flex-start"}>
        <Flex direction={"row"} gap={"sm"}>
          {title}
          {leftSection && (
            <Group justify={"stretch"}>
              <Divider orientation={"vertical"} />
              {leftSection}
            </Group>
          )}
        </Flex>

        <Group>
          {rightSection}
          {hiddenSection && (
            <Popover position={"bottom-end"} width={"fit-content"}>
              <Popover.Target>
                <ActionIcon variant={"subtle"} size={"md"} matchFormSize icon={"ellipsis-vertical"} />
              </Popover.Target>
              <Popover.Dropdown p={"sm"}>
                <Stack gap={"xs"}>{hiddenSection}</Stack>
              </Popover.Dropdown>
            </Popover>
          )}
        </Group>
      </Group>
      {description && <Text>{description}</Text>}
    </Stack>
  )
}

export default PageHeader
