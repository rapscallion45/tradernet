import React, { FC } from "react"
import { Card, Center, CloseButton, Group, Indicator, Stack, Text, Title } from "@mantine/core"
import { AppNotification } from "global/types"
import ActionIcon from "components/ActionIcon/ActionIcon"
import Button from "components/Button/Button"
import classes from "./NotificationDrawer.module.css"
import SideDrawer from "components/layout/SideDrawer/SideDrawer"
import { icon } from "hooks/useToast"
import { useNotificationStorage } from "hooks/useNotificationStorage"
import { useSideDrawer } from "hooks/useSideDrawer"

const NotificationDrawer: FC = () => {
  const { opened, toggle } = useSideDrawer("notifications", "header")
  const [storedNotifications, setStoredNotifications] = useNotificationStorage()

  const color = "light-dark(var(--formpipe-purple-light), var(--formpipe-purple-dark))"

  const panelContent =
    storedNotifications.length > 0 ? (
      <Stack>
        {storedNotifications
          // Filter out any temporary states
          .filter((value) => !value.loading && !value.progress)
          // Reverse the order to show the latest notifications first
          .reverse()
          .map((notification) => (
            <CustomNotification key={`${notification.title}-${notification.timestamp}`} {...notification} />
          ))}
      </Stack>
    ) : (
      <Card m={"lg"} withBorder>
        <Center p={"lg"}>
          <Text fw={700}>No notifications 🌞</Text>
        </Center>
      </Card>
    )
  return (
    <>
      <Indicator size={6} offset={7} position="top-end" color={color} disabled={storedNotifications.length === 0}>
        <ActionIcon variant="subtle" aria-label="Notification Drawer" icon={"bell"} onClick={toggle} data-selected={opened} tooltip={"Notifications"} />
      </Indicator>
      <SideDrawer name={"notifications"} location={"header"} title={"Notifications"}>
        <Stack>
          <Group justify={"space-between"}>
            <Title order={6} pt={5} fz={"sm"} fw={700}>
              LATEST
            </Title>
            <Button leftIcon={"envelope-open"} variant="outline" onClick={() => setStoredNotifications([])} size={"xs"}>
              Mark all as read
            </Button>
          </Group>
          {panelContent}
        </Stack>
      </SideDrawer>
    </>
  )
}

/**
 * Simple representation of a notification for use in the NotificationDrawer.
 */
const CustomNotification: FC<AppNotification> = ({ id, variant, message, title, timestamp }) => {
  const [notificationStorage, setNotificationStorage] = useNotificationStorage()
  // Remove the notification from the storage
  const clearNotification = () => setNotificationStorage(notificationStorage.filter((value) => value.id !== id))
  // TODO timestamp rendering can be improved. This is just a quick and dirty implementation.
  const timestampLabel = `${new Date(timestamp).toLocaleDateString()} ${new Date(timestamp).toLocaleTimeString()}`

  return (
    <Card padding="md" withBorder={false} className={classes.notification}>
      <Stack>
        <Group justify={"space-between"}>
          <Group justify={"flex-start"} align={"flex-start"}>
            <Text>{icon(variant)}</Text>
            <Group justify={"space-between"}>
              <Text size="sm" fw={700}>
                {title}
              </Text>
            </Group>
          </Group>
          <CloseButton size={"sm"} onClick={clearNotification} />
        </Group>

        <Group justify={"space-between"} align={"flex-end"}>
          {typeof message === "string" ? (
            <Text size="xs">{message}</Text>
          ) : (
            <Stack gap={"xs"}>
              {message.map((line) => (
                <Text key={line} fz={"xs"}>
                  {line}
                </Text>
              ))}
            </Stack>
          )}

          <Text size={"xs"} fw={700}>
            {timestampLabel}
          </Text>
        </Group>
      </Stack>
    </Card>
  )
}

export default NotificationDrawer
