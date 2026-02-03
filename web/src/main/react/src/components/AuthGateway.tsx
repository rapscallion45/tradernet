import { FC, PropsWithChildren, useEffect, useMemo, useState } from "react"
import { Navigate, useLocation } from "react-router-dom"
import Routes from "global/Routes"
import { useGlobalStore } from "hooks/useGlobalStore"
import PageLoadingSpinner from "components/PageLoadingSpinner"
import apiClient from "api/apiClient"
import { AuthSessionResponse } from "api/types"

const AuthGateway: FC<PropsWithChildren> = ({ children }) => {
  const location = useLocation()
  const { currentUser, setCurrentUser } = useGlobalStore()
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let isMounted = true

    const loadSession = async () => {
      if (currentUser.username) {
        setReady(true)
        return
      }

      try {
        const response = await apiClient.get<AuthSessionResponse>("/auth/session")
        if (isMounted) {
          setCurrentUser(response.data)
        }
      } catch {
        if (isMounted) {
          setCurrentUser({ username: "" })
        }
      } finally {
        if (isMounted) {
          setReady(true)
        }
      }
    }

    loadSession()

    return () => {
      isMounted = false
    }
  }, [currentUser.username, setCurrentUser])

  const isAuthenticated = useMemo(() => Boolean(currentUser.username), [currentUser.username])

  if (!ready) {
    return <PageLoadingSpinner />
  }

  if (!isAuthenticated) {
    return <Navigate to={Routes.Login} replace state={{ from: location }} />
  }

  return <>{children}</>
}

export default AuthGateway
