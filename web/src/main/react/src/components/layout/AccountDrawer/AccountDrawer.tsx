import { FC } from "react"
import { Group, Stack, Text, Title, Tooltip, Button, ActionIcon } from "@mantine/core"
import { IconExternalLink, IconUserCircle } from "@tabler/icons-react"
import { useLogout } from "hooks/useAuth"
import { useSideDrawer } from "hooks/useSideDrawer"
import useSession from "hooks/useSession"
import SideDrawer from "components/layout/SideDrawer/SideDrawer"

/**
 * User account drawer component
 * @constructor
 */
const AccountDrawer: FC = () => {
  const { toggle } = useSideDrawer("account-info", "header")
  const { isPending, execute: executeLogout } = useLogout()
  const { data: session } = useSession()

  return (
    <>
      <Tooltip label={"Account"}>
        <ActionIcon variant={"subtle"} size={"lg"} aria-label={"Account Drawer"} onClick={toggle}>
          <IconUserCircle size={23} />
        </ActionIcon>
      </Tooltip>
      <SideDrawer
        name={"account-info"}
        title={"Account Info"}
        location={"header"}
        headerContent={
          <Button size={"xs"} onClick={() => executeLogout()} loading={isPending}>
            Sign out
          </Button>
        }>
        <Stack justify={"space-between"}>
          <Title order={6} fz={"sm"} fw={700}>
            ACCOUNT DETAILS
          </Title>
          <Stack>
            <Group>
              Logged on as: <span data-testid={"username"}>{session.username}</span>
            </Group>
          </Stack>
          <Title order={6} pt={"lg"} fz={"sm"} fw={700}>
            GENERAL DOCUMENTATION
          </Title>
          <Group>
            <Text>Click below to view our documentation wiki.</Text>
          </Group>
          <Group>
            <Button
              rightSection={<IconExternalLink size={16} />}
              variant={"outline"}
              onClick={() => window.open("https://github.com/rapscallion45/tradernet/wiki")}>
              View Tradernet Knowledge Base
            </Button>
          </Group>
        </Stack>
      </SideDrawer>
    </>
  )
}
export default AccountDrawer
