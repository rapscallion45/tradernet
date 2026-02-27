import { FC, useMemo } from "react"
import { ColumnDef } from "@tanstack/react-table"
import { Stack } from "@mantine/core"
import { User } from "api/types"
import { CardGrid } from "components/CardGrid/CardGrid"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Table } from "components/Table/Table"
import { Title } from "components/Title/Title"
import { UserCard } from "components/UserCard/UserCard"
import useGroups from "hooks/useGroups"
import useUsers from "hooks/useUsers"

const USER_CARD_GROUPS = new Set(["Super Users", "Administrators"])
const FALLBACK_GROUP_NAME = "Unassigned"

type GroupSection = {
  key: string
  groupName: string
  users: User[]
}

/**
 * Admin users page
 */
const UsersPage: FC = () => {
  const { data: users = [] } = useUsers()
  const { data: groups = [] } = useGroups()

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
    const usersByUsername = new Map(users.map((user) => [user.username, user] as const))

    const sections: GroupSection[] = groups.map((group) => ({
      key: `group-${group.id}`,
      groupName: group.name ?? `Group ${group.id}`,
      users: (group.usernames ?? [])
        .map((username) => usersByUsername.get(username))
        .filter((user): user is User => user !== undefined),
    }))

    const assignedUsernames = new Set(groups.flatMap((group) => group.usernames ?? []))
    const unassignedUsers = users.filter((user) => !assignedUsernames.has(user.username))

    if (unassignedUsers.length > 0) {
      sections.push({
        key: "unassigned",
        groupName: FALLBACK_GROUP_NAME,
        users: unassignedUsers,
      })
    }

    const prioritizedGroupNames = ["Super Users", "Administrators"]

    return sections
      .filter((section) => section.users.length > 0)
      .sort((a, b) => {
        const aPriority = prioritizedGroupNames.indexOf(a.groupName)
        const bPriority = prioritizedGroupNames.indexOf(b.groupName)

        if (aPriority >= 0 || bPriority >= 0) {
          if (aPriority < 0) return 1
          if (bPriority < 0) return -1

          return aPriority - bPriority
        }

        return a.groupName.localeCompare(b.groupName)
      })
  }, [groups, users])

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Users</Title>} description={"View all users and their role assignments."} />

      {groupedUsers.map(({ key, groupName, users: usersInGroup }) => (
        <Stack key={key}>
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
