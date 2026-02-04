import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { QueryClientKeys } from "global/constants"
import { SessionInfo } from "api/types"
import { getRestClient } from "../api/RestClient"

/**
 * Custom hook pertaining logic for retrieving the logged-in User session
 */
const useSession = (): UseSuspenseQueryResult<SessionInfo> => {
  return useSuspenseQuery({
    queryKey: [QueryClientKeys.Session],
    queryFn: () => getRestClient().authResource.getSession(),
    meta: {
      bypassGlobal401: true,
    },
    retry: 1,
  })
}

export default useSession
