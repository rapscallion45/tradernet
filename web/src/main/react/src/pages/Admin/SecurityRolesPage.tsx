import { FC } from "react"
import { Stack } from "@mantine/core"
import { IconShield } from "@tabler/icons-react"
import { ActionCard } from "components/ActionCard/ActionCard"
import { CardGrid } from "components/CardGrid/CardGrid"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Title } from "components/Title/Title"
import useRoles from "hooks/useRoles"

/**
 * Admin security roles page
 */
const SecurityRolesPage: FC = () => {
  const { data: securityRoles = [] } = useRoles()

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Security Roles</Title>} description={"View all system security roles."} />

      <Stack>
        <SectionHeading>SECURITY ROLES</SectionHeading>
        <CardGrid>
          {securityRoles.map((securityRole) => (
            <ActionCard key={securityRole.name} text={securityRole.name} icon={<IconShield />} secondaryText={"Security role"} />
          ))}
        </CardGrid>
      </Stack>
    </Stack>
  )
}

export default SecurityRolesPage
