import React, { forwardRef, ReactNode, useState } from "react"
import { ActionIcon, BoxProps, Card, Group, Menu, Space, Text } from "@mantine/core"
import baseClasses from "./BaseCard.module.css"
import cx from "clsx"
import { MenuItem } from "global/types"
import { IconDotsVertical } from "@tabler/icons-react"

type ClassMap = Record<string, string | undefined>

export function mergeClassMaps(...maps: ClassMap[]): ClassMap {
  const merged: ClassMap = {}
  for (const map of maps) {
    for (const key of Object.keys(map)) {
      merged[key] = cx(merged[key], map[key])
    }
  }
  return merged
}

export type CommonCardProps = {
  onClick?: () => void
  featured?: boolean
  disabled?: boolean
  modified?: boolean
  active?: boolean
  secondaryActions?: MenuItem[]
}

export type BaseCardProps = CommonCardProps &
  BoxProps & {
    classes?: CSSModuleClasses
    children: ReactNode
  }

/**
 * BaseCard is designed to be extended by other card components, providing a consistent foundation.
 * It integrates various features at the base level, such as featured, disabled, modified, and a kebab menu.
 * To ensure accessibility, it uses a button element when clickable and a section element when not.
 *
 * @param onClick Optional. The function to call when the card is clicked. If not provided, the card will not be clickable. Also changes the card to use a button element for accessibility.
 * @param featured Optional. If true, the card will be styled as a featured (purple) card.
 * @param disabled Optional. If true, the card will be styled as disabled and will not be clickable.
 * @param modified Optional. If true, the card will be styled to indicate it has been modified.
 * @param secondaryActions - The actions that go in the kebab menu.
 * @param classes - Custom CSS classes to override default styles.
 * @param children - The content of the card.
 * @param rest - Other BoxProps to pass to the Card component.
 *
 * @see BaseActionCard
 * @see UserCard
 */
export const BaseCard = forwardRef<HTMLButtonElement, BaseCardProps>(
  ({ onClick, featured = false, disabled = false, modified = false, active = false, classes, children, secondaryActions, ...rest }, ref) => {
    const clickable = !!onClick && !disabled
    const [opened, setOpened] = useState(false)

    return (
      <Card
        ref={ref}
        // only set type when it's actually a button
        component={clickable ? "button" : "section"}
        type={clickable ? "button" : undefined}
        role={clickable ? "button" : undefined}
        mod={{ clickable, modified, active, featured, disabled }}
        classNames={mergeClassMaps(baseClasses, classes || {})}
        onClick={!disabled ? onClick : undefined}
        {...rest}>
        {children}
        {/* Kebab Menu */}
        {!(disabled || featured) && secondaryActions && (
          <Menu shadow="md" position={"bottom-start"} opened={opened} onChange={setOpened}>
            <Menu.Target>
              <ActionIcon
                aria-label={"Secondary Actions"}
                variant={"default"}
                className={baseClasses.hoverIcon}
                tabIndex={0}
                onClick={(event) => {
                  event.stopPropagation()
                  setOpened(!opened)
                }}>
                <IconDotsVertical />
              </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
              {secondaryActions.map((item, index) => (
                <Menu.Item
                  miw={150}
                  key={index}
                  leftSection={
                    <Group w={24} align={"center"} justify={"center"}>
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
        )}
      </Card>
    )
  },
)
