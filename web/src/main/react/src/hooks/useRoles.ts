import { useQuery, UseQueryResult } from "@tanstack/react-query"
import { Role } from "api/Roles"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"

/**
 * Custom hook for retrieving all roles
 */
const useRoles = (enabled = true): UseQueryResult<Role[]> => {
  return useQuery({
    queryKey: [QueryClientKeys.Roles],
    queryFn: () => getRestClient().rolesResource.getRoles(),
    enabled,
  })
}

export default useRoles
