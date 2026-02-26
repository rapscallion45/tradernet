import { FC } from "react"
import { Stack } from "@mantine/core"
import { IconUsersGroup } from "@tabler/icons-react"
import { ActionCard } from "components/ActionCard/ActionCard"
import { CardGrid } from "components/CardGrid/CardGrid"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Title } from "components/Title/Title"
import useGroups from "hooks/useGroups"

/**
 * Admin groups page
 */
const GroupsPage: FC = () => {
  const { data: groups = [] } = useGroups()

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Groups</Title>} description={"View all user groups in the system."} />

      <Stack>
        <SectionHeading>GROUPS</SectionHeading>
        <CardGrid>
          {groups.map((group) => (
            <ActionCard key={group.id} text={`Group ${group.id}`} icon={<IconUsersGroup />} secondaryText={"User group"} />
          ))}
        </CardGrid>
      </Stack>
    </Stack>
  )
}

export default GroupsPage
