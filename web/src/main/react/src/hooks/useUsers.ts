import { useQuery, UseQueryResult } from "@tanstack/react-query"
import { User } from "api/types"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"

/**
 * Custom hook for retrieving all users
 */
const useUsers = (enabled = true): UseQueryResult<User[]> => {
  return useQuery({
    queryKey: [QueryClientKeys.Users],
    queryFn: () => getRestClient().usersResource.getUsers(),
    enabled,
  })
}

export default useUsers
