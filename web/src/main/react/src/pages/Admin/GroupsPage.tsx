import { FC, useEffect, useMemo, useState } from "react"
import { Button, MultiSelect, Stack, Text } from "@mantine/core"
import { IconUsersGroup } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { ActionCard } from "components/ActionCard/ActionCard"
import { CardGrid } from "components/CardGrid/CardGrid"
import SideDrawer from "components/layout/SideDrawer/SideDrawer"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Title } from "components/Title/Title"
import useGroups from "hooks/useGroups"
import useRoles from "hooks/useRoles"
import { useSideDrawer } from "hooks/useSideDrawer"
import useUsers from "hooks/useUsers"
import { QueryClientKeys } from "global/constants"

/**
 * Admin groups page
 */
const GroupsPage: FC = () => {
  const { data: groups = [] } = useGroups()
  const { data: users = [] } = useUsers()
  const { data: roles = [] } = useRoles()
  const queryClient = useQueryClient()
  const { open, close } = useSideDrawer("group-details", "right")

  const [selectedGroupId, setSelectedGroupId] = useState<number>()
  const [selectedUsernames, setSelectedUsernames] = useState<string[]>([])
  const [selectedRoleNames, setSelectedRoleNames] = useState<string[]>([])

  const selectedGroup = useMemo(() => groups.find((group) => group.id === selectedGroupId), [groups, selectedGroupId])

  useEffect(() => {
    if (!selectedGroup) return
    setSelectedUsernames(selectedGroup.usernames ?? [])
    setSelectedRoleNames(selectedGroup.roleNames ?? [])
  }, [selectedGroup])

  const updateGroup = useMutation({
    mutationFn: () =>
      getRestClient().groupsResource.updateGroup(selectedGroupId as number, {
        usernames: selectedUsernames,
        roleNames: selectedRoleNames,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Groups] })
      queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Users] })
      close()
    },
  })

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Groups</Title>} description={"View all user groups in the system."} />

      <Stack>
        <SectionHeading>GROUPS</SectionHeading>
        <CardGrid>
          {groups.map((group) => (
            <ActionCard
              key={group.id}
              text={`Group ${group.id}`}
              icon={<IconUsersGroup />}
              secondaryText={"User group"}
              onClick={() => {
                setSelectedGroupId(group.id)
                open()
              }}
            />
          ))}
        </CardGrid>
      </Stack>

      <SideDrawer
        name={"group-details"}
        location={"right"}
        title={selectedGroup ? `Group ${selectedGroup.id}` : "Group"}
        onClose={() => {
          setSelectedGroupId(undefined)
          close()
        }}
        footer={{
          content: (
            <Button onClick={() => updateGroup.mutate()} disabled={!selectedGroupId || updateGroup.isPending} loading={updateGroup.isPending} fullWidth>
              Save Group
            </Button>
          ),
        }}>
        <Stack>
          <Text size={"sm"} c={"dimmed"}>
            Configure which users belong to this group and which security roles the group grants.
          </Text>
          <MultiSelect
            label={"Group Users"}
            placeholder={"Select users"}
            data={users.map((user) => ({ label: user.username, value: user.username }))}
            value={selectedUsernames}
            onChange={setSelectedUsernames}
            searchable
          />
          <MultiSelect
            label={"Security Roles"}
            placeholder={"Select roles"}
            data={roles.map((role) => ({ label: role.name, value: role.name }))}
            value={selectedRoleNames}
            onChange={setSelectedRoleNames}
            searchable
          />
        </Stack>
      </SideDrawer>
    </Stack>
  )
}

export default GroupsPage
