import { FC } from "react"
import { useNavigate } from "react-router-dom"
import { useLogin } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import { LoginData } from "api/types"
import Routes from "global/Routes"
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from "global/constants"
import LoginForm from "./forms/LoginForm"

/**
 * Application login page
 */
const LoginPage: FC = () => {
  const loginMutation = useLogin()
  const { setCurrentUser } = useGlobalStore()
  const navigate = useNavigate()

  const onSubmit = async (data: LoginData) => {
    try {
      const response = await loginMutation.mutateAsync(data)
      localStorage.setItem(AUTH_TOKEN_KEY, response.token)
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(response.user))
      setCurrentUser(response.user)
      navigate(Routes.Dashboard)
    } catch {
      alert("Login failed")
    }
  }

  return <LoginForm onSubmit={onSubmit} />
}

export default LoginPage
