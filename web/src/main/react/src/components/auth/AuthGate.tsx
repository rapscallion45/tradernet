import { FC } from "react"
import { Outlet } from "react-router-dom"
import AccessDenied from "components/AccessDenied"
import useCurrentUser from "hooks/useCurrentUser"

const allowedRoles = new Set(["SUPER USER", "ADMIN"])

/**
 * Route guard for Admin pages
 */
const AuthGate: FC = () => {
  const { data: currentUser } = useCurrentUser()
  const hasAdminAccess = (currentUser.roleNames ?? []).some((role) => allowedRoles.has(role))

  if (!hasAdminAccess) {
    return <AccessDenied height={"calc(100vh - 200px)"} />
  }

  return <Outlet />
}

export default AuthGate
