import { ReactNode, useCallback } from "react"
import { NotificationData, notifications, notificationsStore, useNotifications } from "@mantine/notifications"
import { Progress, Stack, Text } from "@mantine/core"
import { IconAlertTriangle, IconCircleCheck, IconCircleLetterI } from "@tabler/icons-react"
import { AppNotification, AppNotificationVariant } from "global/types"
import { useNotificationStorage } from "hooks/useNotificationStorage"

/**
 * Hook to show a basic toast notification at the top of the screen.
 *
 * For anything more complex you can call Mantine directly.
 */
const useToast = (): { toast: (notif: AppNotification) => void } => {
  // Mantine notifications state, that contains any currently showing notifications, and any in the queue.
  const notificationsState = useNotifications() //Not sure when we stopped using this, but something to fix/remove as appropriate
  // Local storage for notifications. We add any non-temporary notifications to this storage so they can be viewed later.
  const [, setNotificationStorage] = useNotificationStorage()

  /**
   * Function to show a toast notification.
   *
   * Notifications can be in a loading state, show progress, and have different categories.
   * If a notification is for an error, or still in some kind of loading state, it will not auto-close.
   *
   * @param id Unique ID of the notification. We always require an ID to prevent duplicates and allow for updates.
   * @param category Determines the icon and styling by setting the variant. Possible values: "info", "success", "error".
   * @param message The message to display in the notification, either a string or an array of strings to be shown on separate lines.
   * @param title Title of the notification. If not provided, we use a default based on the category.
   * @param loading Whether the notification should show a loading spinner.
   * @param progress The progress of the notification, as a percentage. If provided, we show a progress bar.
   */
  const toast = useCallback(
    ({ id, variant, message, title, loading, progress, timestamp }: AppNotification) => {
      console.log("Re-declare")
      // If we didn't receive a title, just use a simple default
      const finalTitle = title ?? variant.charAt(0).toUpperCase() + variant.slice(1)

      // Check if a notification with the same ID already exists
      const notificationCurrentlyShowing = notificationsStore.getState().notifications.find((nfn) => nfn.id === id)
      //const notificationExistsInStorage = notificationStorage.find((nfn) => nfn.id === id)

      const data: AppNotification = {
        id,
        title: finalTitle,
        message,
        variant,
        loading,
        timestamp,
      }

      // Does the notification need to be updated in the future?
      // If showing a loading state or the progress bar is not yet at 100%, we won't auto-close because we are going to update at some point
      const needsUpdateInFuture = loading || (progress && progress !== 100)

      /**
       * Add the notification to the stored notifications in local storage, but only if it isn't a temporary state.
       * If a notification with the id already exists, we replace it, otherwise we add it.
       */
      if (!needsUpdateInFuture) {
        setNotificationStorage((currentNotificationStorage) => {
          const notificationExistsInStorage = currentNotificationStorage.find((nfn) => nfn.id === id)
          if (notificationExistsInStorage) {
            return currentNotificationStorage.map((nfn) => (nfn.id === id ? data : nfn))
          } else {
            return [...currentNotificationStorage, data]
          }
        })
      }

      // Now proceed to showing the notification...

      // If we received a progress value, we need to update the content to include a progress bar...
      const content = progress ? (
        <Stack gap={"xs"}>
          <Text fz={"sm"}>{message}</Text>
          <Progress value={progress} variant={variant} />
        </Stack>
      ) : // ...no progress value, just show the message...
      typeof message === "string" ? (
        message
      ) : (
        // ...unless it's a string array, in which case we show each line separately.
        <Stack gap={"xs"}>
          {message.map((line) => (
            <Text key={line} fz={"sm"}>
              {line}
            </Text>
          ))}
        </Stack>
      )

      // Construct the options object
      const options: NotificationData = {
        ...data,
        message: content,
        // if we need an update in the future, or it's an error, don't auto-close, otherwise close fairly quickly
        autoClose: needsUpdateInFuture || variant === "error" ? false : 3000,
        icon: icon(variant),
      }

      // Update or show based on presence of existing notification with same ID
      notificationCurrentlyShowing ? notifications.update(options) : notifications.show(options)
    },
    [setNotificationStorage],
  )

  return { toast }
}

// Determine the icon based on the category of the notification
function icon(variant: AppNotificationVariant): ReactNode {
  return variant === "error" ? <IconAlertTriangle /> : variant === "success" ? <IconCircleCheck /> : <IconCircleLetterI />
}

export { useToast, icon }
