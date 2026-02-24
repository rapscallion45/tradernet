import { useSuspenseQuery, UseSuspenseQueryResult } from "@tanstack/react-query"
import { QueryClientKeys } from "global/constants"
import { HealthResponse, SessionInfo } from "api/types"
import { getRestClient } from "api/RestClient"

/**
 * Custom hook pertaining logic for retrieving the health status of API
 */
const useHealthCheck = (): UseSuspenseQueryResult<HealthResponse> => {
  return useSuspenseQuery({
    queryKey: [QueryClientKeys.HealthCheck],
    queryFn: () => getRestClient().healthResource.get(),
  })
}

export default useHealthCheck
