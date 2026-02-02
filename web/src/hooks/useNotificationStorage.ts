import { useLocalStorage } from "@mantine/hooks"
import { AppNotification } from "global/types"

/**
 * Hook to store notifications in local storage.
 */
export const useNotificationStorage = () => {
  return useLocalStorage<AppNotification[]>({
    key: "formpipe-notifications",
    defaultValue: [],
  })
}
