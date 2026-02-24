"use client"

import { FC, useState, PropsWithChildren, ChangeEvent, useEffect, ReactNode } from "react"
import { Box, Title, Text, Collapse, ActionIcon, Popover, List, ListItem, Card, Divider, Checkbox, Grid, Flex, Tooltip } from "@mantine/core"
import { useDisclosure, useClickOutside } from "@mantine/hooks"
import classes from "./ExpandablePanel.module.css"
import { AppLength } from "global/types"
import { IconArrowDown, IconArrowUp, IconDotsVertical } from "@tabler/icons-react"

/**
 * Panel action callback type
 */
export type PanelAction = {
  label: string
  icon: ReactNode
  callback: () => void
}

/**
 * Expandable Panel Props type
 */
export type ExpandablePanelProps = {
  title: string
  subtitle?: string
  isOpen?: boolean
  icon?: ReactNode
  toggleOpenOnGrid?: boolean
  actions?: PanelAction[]
  showActionTooltips?: boolean
  panelCentreContent?: ReactNode
  disabled?: boolean
  width?: AppLength
  collapseOnClickAway?: boolean
  showCheckbox?: boolean
  isChecked?: boolean
  onCheckboxChange?: (value: boolean) => void
} & PropsWithChildren

/**
 * Expandable Panel
 *
 * @param title - panel title
 * @param subtitle - panel subtitle
 * @param isOpen - allows parent to set panel open state
 * @param panelIcon - panel main icon to be displayed next to title
 * @param toggleOpenOnGrid - panel will toggle open state when grid area is clicked
 * @param actions - panel action callbacks to be displayed in panel popover menu
 * @param showActionTooltips - display action label in  actionIcon tooltips
 * @param panelCentreContent - component to be rendered in centre of panel, next to title
 * @param disabled - panel expansion disabled
 * @param width - desired width of panel, either number or string (i.e. "200px")
 * @param collapseOnClickAway - panel will collapse when user clicks off
 * @param showCheckbox - display panel checkbox
 * @param isChecked - current value of checkbox
 * @param onCheckboxChange - panel checkbox change callback handler
 * @param children - child node(s)
 */
export const ExpandablePanel: FC<ExpandablePanelProps> = ({
  title,
  subtitle,
  isOpen = false,
  icon,
  toggleOpenOnGrid = false,
  actions = [],
  showActionTooltips = false,
  panelCentreContent,
  disabled = false,
  width = "auto",
  collapseOnClickAway = false,
  showCheckbox = false,
  isChecked = false,
  onCheckboxChange,
  children,
}) => {
  const [opened, { open, close, toggle }] = useDisclosure(isOpen)
  const [popoverOpened, { close: closePopover, toggle: togglePopover }] = useDisclosure(false)
  const [popoverControl, setPopoverControl] = useState<HTMLElement | null>(null)
  const [popoverDropdown, setPopoverDropdown] = useState<HTMLElement | null>(null)
  const ref = useClickOutside(() => (collapseOnClickAway ? close() : null))
  useClickOutside(() => closePopover(), null, [popoverControl, popoverDropdown])

  /** Handle updating of isOpen state from external source */
  useEffect(() => (isOpen ? open() : close()), [isOpen, open, close])

  /**
   * Handle registered action clicks
   * @param callback - registered callback handler of clicked action
   */
  const handleActionClick = (callback: () => void) => {
    callback?.()
    closePopover()
  }

  /**
   * Handle panel selection change event
   * @param event checkbox change event
   */
  const handleSelectChange = (event: ChangeEvent<HTMLInputElement>) => {
    onCheckboxChange?.(event.target.checked)
  }

  return (
    <Card ref={ref} w={width} shadow="md">
      <Grid grow align={"center"} onClick={toggleOpenOnGrid ? toggle : undefined} style={{ cursor: toggleOpenOnGrid ? "pointer" : "default" }}>
        {showCheckbox && (
          <Grid.Col span="content" style={{ maxWidth: "35px" }}>
            <Checkbox checked={isChecked} onChange={handleSelectChange} disabled={disabled} size="14px" p={4} />
          </Grid.Col>
        )}
        {icon && (
          <Grid.Col span="content" style={{ maxWidth: "35px" }}>
            {icon}
          </Grid.Col>
        )}
        <Grid.Col span="content">
          <Title order={5}>{title}</Title>
          {subtitle && (
            <Title order={6} className={classes.subtitle}>
              {subtitle}
            </Title>
          )}
        </Grid.Col>
        {panelCentreContent && <Grid.Col span="content">{panelCentreContent}</Grid.Col>}
        <Grid.Col span="content">
          <Flex justify="flex-end" direction="row" gap="sm">
            {actions.length > 0 &&
              actions.map((action: PanelAction) => (
                <Box visibleFrom={"sm"}>
                  {showActionTooltips ? (
                    <Tooltip label={action.label}>
                      <ActionIcon
                        key={action.label}
                        title={undefined}
                        onClick={(e) => {
                          e.stopPropagation()
                          action.callback()
                        }}
                        variant="subtle"
                        aria-label={`${title}-${action.label}-panel-action`}
                        disabled={disabled}
                        className={classes.actionIcon}>
                        {action.icon}
                      </ActionIcon>
                    </Tooltip>
                  ) : (
                    <ActionIcon
                      title={undefined}
                      onClick={(e) => {
                        e.stopPropagation()
                        action.callback()
                      }}
                      variant="subtle"
                      aria-label={`${title}-${action.label}-panel-action`}
                      disabled={disabled}
                      className={classes.actionIcon}>
                      {action.icon}
                    </ActionIcon>
                  )}
                </Box>
              ))}
            <ActionIcon onClick={toggle} variant="subtle" aria-label={`${title}-panel-button`} disabled={disabled} className={classes.actionIcon}>
              {opened ? <IconArrowUp /> : <IconArrowDown />}
            </ActionIcon>
            {actions.length > 0 && (
              <Box hiddenFrom={"sm"}>
                <Popover opened={popoverOpened} closeOnClickOutside position="bottom" withArrow shadow="md">
                  <Popover.Target ref={setPopoverControl}>
                    <ActionIcon
                      onClick={togglePopover}
                      variant="subtle"
                      aria-label={`${title}-panel-popover-action`}
                      disabled={disabled}
                      className={classes.actionIcon}>
                      <IconDotsVertical />
                    </ActionIcon>
                  </Popover.Target>
                  <Popover.Dropdown ref={setPopoverDropdown} aria-label={`${title}-panel-popover-content`} hiddenFrom={"sm"}>
                    <List spacing="xs">
                      {actions.map((action) => (
                        <ListItem
                          key={action.label}
                          onClick={() => handleActionClick(action.callback)}
                          className={classes.actionItem}
                          icon={
                            <Flex justify={"center"} miw={20}>
                              {action.icon}
                            </Flex>
                          }>
                          <Text size="md" className={classes.actionIconText}>
                            {action.label}
                          </Text>
                        </ListItem>
                      ))}
                    </List>
                  </Popover.Dropdown>
                </Popover>
              </Box>
            )}
          </Flex>
        </Grid.Col>
      </Grid>
      <Collapse in={opened} aria-label={`${title}-panel-content`}>
        <Divider mt={20} />
        <Box mt={10}>{children}</Box>
      </Collapse>
    </Card>
  )
}
