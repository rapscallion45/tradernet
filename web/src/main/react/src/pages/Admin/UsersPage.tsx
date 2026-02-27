import { FC } from "react"
import { Stack } from "@mantine/core"
import { CardGrid } from "components/CardGrid/CardGrid"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Title } from "components/Title/Title"
import { UserCard } from "components/UserCard/UserCard"
import useUsers from "hooks/useUsers"

/**
 * Admin users page
 */
const UsersPage: FC = () => {
  const { data: users = [] } = useUsers()

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Users</Title>} description={"View all users and their role assignments."} />

      <Stack>
        <SectionHeading>USERS</SectionHeading>
        <CardGrid>
          {users.map((user) => (
            // roleNames represent security role assignments on each user
            <UserCard
              key={user.id ?? user.username}
              username={user.username}
              fullName={user.fullName}
              groups={user.roleNames ?? []}
              isAdmin={(user.roleNames ?? []).some(
                (role) => role === "ALL Rights" || role === "Admin Rights",
              )}
            />
          ))}
        </CardGrid>
      </Stack>
    </Stack>
  )
}

export default UsersPage
