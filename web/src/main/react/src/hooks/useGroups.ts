import { useQuery, UseQueryResult } from "@tanstack/react-query"
import { Group } from "api/Groups"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"

/**
 * Custom hook for retrieving all groups
 */
const useGroups = (enabled = true): UseQueryResult<Group[]> => {
  return useQuery({
    queryKey: [QueryClientKeys.Groups],
    queryFn: () => getRestClient().groupsResource.getGroups(),
    enabled,
  })
}

export default useGroups
