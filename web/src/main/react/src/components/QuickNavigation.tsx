import { FC } from "react"
import { useNavigate } from "react-router-dom"
import { Spotlight, SpotlightActionData, SpotlightActionGroupData } from "@mantine/spotlight"
import Routes from "global/Routes"
import classes from "./QuickNavigation.module.css"
import { IconHome, IconSearch, IconUser, IconUsersGroup } from "@tabler/icons-react"

/** default icon size */
const iconSize = "lg"

/**
 * Quick navigation component
 */
const QuickNavigation: FC = () => {
  const navigate = useNavigate()

  const baseActions: SpotlightActionData[] = [
    // Dashboard
    {
      id: "home",
      label: "Home",
      description: "Go to your dashboard",
      onClick: () => navigate(Routes.Dashboard),
      leftSection: <IconHome />,
    },
    {
      id: "users",
      label: "Users",
      description: "View users",
      onClick: () => navigate(Routes.AdminUsers),
      leftSection: <IconUser />,
    },
    {
      id: "groups",
      label: "Groups",
      description: "View groups",
      onClick: () => navigate(Routes.AdminGroups),
      leftSection: <IconUsersGroup />,
    },
  ]

  // Create action groups based on user role settings
  const actionsList: SpotlightActionGroupData[] = [
    {
      group: "Pages",
      actions: [...baseActions],
    },
  ]

  return (
    <Spotlight
      classNames={classes}
      shortcut={["Mod + K", "Mod + Shift + F"]}
      actions={actionsList}
      nothingFound={"Nothing found..."}
      highlightQuery
      limit={7}
      searchProps={{
        leftSection: <IconSearch />,
        placeholder: "Search...",
      }}
    />
  )
}

export default QuickNavigation
