import { FC } from "react"
import { Outlet } from "react-router-dom"
import AccessDenied from "components/AccessDenied"
import useCurrentUser from "hooks/useCurrentUser"

type AuthGateProps = {
  requiredRoles?: string[]
}

const defaultRequiredRoles = ["SUPER USER", "ADMIN"]

/**
 * Route guard for Admin pages
 */
const AuthGate: FC<AuthGateProps> = ({ requiredRoles = defaultRequiredRoles }) => {
  const { data: currentUser } = useCurrentUser()
  const allowedRoles = new Set(requiredRoles)
  const hasRequiredAccess = (currentUser.roleNames ?? []).some((role) => allowedRoles.has(role))

  if (!hasRequiredAccess) {
    return <AccessDenied height={"calc(100vh - 200px)"} />
  }

  return <Outlet />
}

export default AuthGate
