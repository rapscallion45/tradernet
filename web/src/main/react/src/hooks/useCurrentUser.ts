import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { User } from "api/types"
import { getRestClient } from "api/RestClient"
import { QueryClientKeys } from "global/constants"
import useSession from "hooks/useSession"

/**
 * Custom hook for retrieving the currently logged-in user's full profile
 */
const useCurrentUser = (): UseSuspenseQueryResult<User> => {
  const { data: session } = useSession()

  return useSuspenseQuery({
    queryKey: [QueryClientKeys.Users, "current", session.username],
    queryFn: () => getRestClient().usersResource.getByUsername(session.username),
  })
}

export default useCurrentUser
