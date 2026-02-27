import { FC, useMemo } from "react"
import { ColumnDef } from "@tanstack/react-table"
import { Stack } from "@mantine/core"
import { CardGrid } from "components/CardGrid/CardGrid"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Table } from "components/Table/Table"
import { Title } from "components/Title/Title"
import { UserCard } from "components/UserCard/UserCard"
import { User } from "api/types"
import useUsers from "hooks/useUsers"

const USER_CARD_GROUPS = new Set(["Super Users", "Administrators"])
const FALLBACK_GROUP_NAME = "Unassigned"

/**
 * Admin users page
 */
const UsersPage: FC = () => {
  const { data: users = [] } = useUsers()

  const columns = useMemo<ColumnDef<User>[]>(
    () => [
      { accessorKey: "username", header: "Username" },
      { accessorKey: "fullName", header: "Full Name" },
      { accessorKey: "emailAddress", header: "Email Address" },
      { accessorKey: "accountExpiry", header: "Account Expiry" },
      { accessorKey: "lastLogin", header: "Last Login" },
      { accessorKey: "changePasswordNextLogin", header: "Change Password Next Login" },
    ],
    [],
  )

  const groupedUsers = useMemo(() => {
    const groupMap = new Map<string, User[]>()

    users.forEach((user) => {
      const userGroups = user.roleNames?.length ? user.roleNames : [FALLBACK_GROUP_NAME]

      userGroups.forEach((groupName) => {
        const existingGroup = groupMap.get(groupName) ?? []
        existingGroup.push(user)
        groupMap.set(groupName, existingGroup)
      })
    })

    const allGroupNames = Array.from(groupMap.keys()).sort((a, b) => a.localeCompare(b))
    const prioritizedGroupNames = ["Super Users", "Administrators", ...allGroupNames]

    return prioritizedGroupNames
      .filter((groupName, index, source) => source.indexOf(groupName) === index)
      .filter((groupName) => groupMap.has(groupName))
      .map((groupName) => ({ groupName, users: groupMap.get(groupName) ?? [] }))
  }, [users])

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Users</Title>} description={"View all users and their role assignments."} />

      {groupedUsers.map(({ groupName, users: usersInGroup }) => (
        <Stack key={groupName}>
          <SectionHeading>{groupName}</SectionHeading>

          {USER_CARD_GROUPS.has(groupName) ? (
            <CardGrid>
              {usersInGroup.map((user) => (
                <UserCard
                  key={`${groupName}-${user.id ?? user.username}`}
                  username={user.username}
                  fullName={user.fullName}
                  groups={user.roleNames ?? []}
                  isAdmin={(user.roleNames ?? []).some(
                    (role) => role === "ALL Rights" || role === "Admin Rights",
                  )}
                />
              ))}
            </CardGrid>
          ) : (
            <Table<User>
              columns={columns}
              data={usersInGroup}
              sorting={{
                state: [],
                setSortingState: () => undefined,
              }}
            />
          )}
        </Stack>
      ))}
    </Stack>
  )
}

export default UsersPage
