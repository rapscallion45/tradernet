import { FC, useEffect, useMemo, useState } from "react"
import { Button, MultiSelect, Stack, Text } from "@mantine/core"
import { IconShield } from "@tabler/icons-react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { ActionCard } from "components/ActionCard/ActionCard"
import { CardGrid } from "components/CardGrid/CardGrid"
import SideDrawer from "components/layout/SideDrawer/SideDrawer"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { Title } from "components/Title/Title"
import { QueryClientKeys } from "global/constants"
import useRoleResources from "hooks/useRoleResources"
import useRoles from "hooks/useRoles"
import { useSideDrawer } from "hooks/useSideDrawer"

/**
 * Admin security roles page
 */
const SecurityRolesPage: FC = () => {
  const { data: securityRoles = [] } = useRoles()
  const { data: resourceEntities = [] } = useRoleResources()
  const queryClient = useQueryClient()
  const { open, close } = useSideDrawer("security-role-details", "right")

  const [selectedRoleName, setSelectedRoleName] = useState<string>()
  const [selectedResources, setSelectedResources] = useState<string[]>([])

  const selectedRole = useMemo(() => securityRoles.find((role) => role.name === selectedRoleName), [securityRoles, selectedRoleName])

  useEffect(() => {
    if (!selectedRole) return
    setSelectedResources(selectedRole.resourceNames ?? [])
  }, [selectedRole])

  const updateRole = useMutation({
    mutationFn: () => getRestClient().rolesResource.updateRole(selectedRoleName as string, { resourceNames: selectedResources }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Roles] })
      close()
    },
  })

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Security Roles</Title>} description={"View all system security roles."} />

      <Stack>
        <SectionHeading>SECURITY ROLES</SectionHeading>
        <CardGrid>
          {securityRoles.map((securityRole) => (
            <ActionCard
              key={securityRole.name}
              text={securityRole.name}
              icon={<IconShield />}
              secondaryText={"Security role"}
              onClick={() => {
                setSelectedRoleName(securityRole.name)
                open()
              }}
            />
          ))}
        </CardGrid>
      </Stack>

      <SideDrawer
        name={"security-role-details"}
        location={"right"}
        title={selectedRole ? `${selectedRole.name}` : "Security Role"}
        onClose={() => {
          setSelectedRoleName(undefined)
          close()
        }}
        footer={{
          content: (
            <Button onClick={() => updateRole.mutate()} disabled={!selectedRoleName || updateRole.isPending} loading={updateRole.isPending} fullWidth>
              Save Role Access
            </Button>
          ),
        }}>
        <Stack>
          <Text size={"sm"} c={"dimmed"}>
            Configure which system entities/resources this security role can access.
          </Text>
          <MultiSelect
            label={"Accessible Entities"}
            placeholder={"Select entities/resources"}
            data={resourceEntities.map((resourceEntity) => ({ label: resourceEntity, value: resourceEntity }))}
            value={selectedResources}
            onChange={setSelectedResources}
            searchable
          />
        </Stack>
      </SideDrawer>
    </Stack>
  )
}

export default SecurityRolesPage
