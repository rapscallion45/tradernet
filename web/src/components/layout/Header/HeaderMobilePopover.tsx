import { FC, ReactNode } from "react"
import { Box, Popover } from "@mantine/core"
import ActionIcon from "components/ActionIcon/ActionIcon"
import { useDisclosure, useClickOutside } from "@mantine/hooks"

/**
 * Header Mobile Popover props
 * @param rightSection - right section component of parent Header to be rendered in popover
 */
export type HeaderMobilePopoverProps = {
  rightSection: ReactNode
}

/**
 * Header Mobile Popover component that renders header's "right section" on small viewports (below "sm")
 */
const HeaderMobilePopover: FC<HeaderMobilePopoverProps> = ({ rightSection }) => {
  const [opened, { toggle, close }] = useDisclosure(false)
  const ref = useClickOutside(() => close())

  return (
    <Box hiddenFrom={"sm"}>
      <Popover opened={opened} position={"bottom"}>
        <Popover.Target>
          <ActionIcon icon={"ellipsis-vertical"} variant={"subtle"} onClick={toggle} />
        </Popover.Target>
        <Popover.Dropdown hiddenFrom={"sm"} ref={ref}>
          {rightSection}
        </Popover.Dropdown>
      </Popover>
    </Box>
  )
}

export default HeaderMobilePopover
