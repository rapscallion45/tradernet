import { FC } from "react"
import { Stack } from "@mantine/core"
import { IconUsersGroup } from "@tabler/icons-react"
import { ActionCard } from "components/ActionCard/ActionCard"
import { CardGrid } from "components/CardGrid/CardGrid"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Title } from "components/Title/Title"
import useRoles from "hooks/useRoles"

/**
 * Admin groups page
 */
const GroupsPage: FC = () => {
  const { data: roles = [] } = useRoles()

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Groups</Title>} description={"View all role groups in the system."} />

      <Stack>
        <SectionHeading>GROUPS</SectionHeading>
        <CardGrid>
          {roles.map((role) => (
            <ActionCard key={role.name} text={role.name} icon={<IconUsersGroup />} secondaryText={"Role group"} />
          ))}
        </CardGrid>
      </Stack>
    </Stack>
  )
}

export default GroupsPage
