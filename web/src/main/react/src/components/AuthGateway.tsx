import { FC, PropsWithChildren, useEffect, useMemo, useState } from "react"
import { Navigate, useLocation } from "react-router-dom"
import Routes from "global/Routes"
import { AUTH_USER_KEY } from "global/constants"
import { useGlobalStore } from "hooks/useGlobalStore"
import PageLoadingSpinner from "components/PageLoadingSpinner"

const AuthGateway: FC<PropsWithChildren> = ({ children }) => {
  const location = useLocation()
  const { currentUser, setCurrentUser } = useGlobalStore()
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (!currentUser.username) {
      const storedUser = localStorage.getItem(AUTH_USER_KEY)
      if (storedUser) {
        setCurrentUser(JSON.parse(storedUser))
      }
    }
    setReady(true)
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
