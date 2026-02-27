import { useQuery, UseQueryResult } from "@tanstack/react-query"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"

/**
 * Retrieves all protected resource entities available for role assignment.
 */
const useRoleResources = (enabled = true): UseQueryResult<string[]> => {
  return useQuery({
    queryKey: [QueryClientKeys.ResourceEntities],
    queryFn: () => getRestClient().rolesResource.getResources(),
    enabled,
  })
}

export default useRoleResources
