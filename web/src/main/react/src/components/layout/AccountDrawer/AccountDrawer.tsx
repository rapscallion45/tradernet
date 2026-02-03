import { FC } from "react"
import { Group, Stack, Text, Title, Tooltip, Button, ActionIcon } from "@mantine/core"
import { IconUserCircle } from "@tabler/icons-react"
import { useLogout } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import { useSideDrawer } from "hooks/useSideDrawer"
import SideDrawer from "components/layout/SideDrawer/SideDrawer"

/**
 * User account drawer component
 * @constructor
 */
const AccountDrawer: FC = () => {
  const { toggle } = useSideDrawer("account-info", "header")
  const logout = useLogout()
  const { currentUser } = useGlobalStore()

  return (
    <>
      <Tooltip label={"Account"}>
        <ActionIcon variant={"subtle"} aria-label={"Account Drawer"} onClick={toggle}>
          <IconUserCircle size={40} />
        </ActionIcon>
      </Tooltip>
      <SideDrawer
        name={"account-info"}
        title={"Account Info"}
        location={"header"}
        headerContent={
          <Button size={"xs"} onClick={() => logout.mutateAsync()} loading={logout.isPending}>
            Sign out
          </Button>
        }>
        <Stack justify={"space-between"}>
          <Title order={6} fz={"sm"} fw={700}>
            ACCOUNT DETAILS
          </Title>
          <Stack>
            <Group>
              Logged on as: <span data-testid={"username"}>{currentUser.username}</span>
            </Group>
          </Stack>
          <Title order={6} pt={"lg"} fz={"sm"} fw={700}>
            GENERAL DOCUMENTATION
          </Title>
          <Group>
            <Text>Click below to view our documentation hub.</Text>
          </Group>
          <Group>
            <Button rightSection={"arrow-up-right-from-square"} variant={"outline"} onClick={() => window.open("https://kb.lasernet.online/docs/lasernet")}>
              View Tradernet Knowledge Base
            </Button>
          </Group>
        </Stack>
      </SideDrawer>
    </>
  )
}
export default AccountDrawer
