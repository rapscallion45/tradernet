import { FC } from "react"
import { useNavigate } from "react-router-dom"
import { useLogin } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import { LoginData } from "api/types"
import Routes from "global/Routes"
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
      const responseUrl = response.request?.responseURL
      if (responseUrl?.includes(Routes.ResetPassword)) {
        window.location.assign(responseUrl)
        return
      }
      setCurrentUser(response.data.user)
      navigate(Routes.Dashboard)
    } catch {
      alert("Login failed")
    }
  }

  return <LoginForm onSubmit={onSubmit} />
}

export default LoginPage
