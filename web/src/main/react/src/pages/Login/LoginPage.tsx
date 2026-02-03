import { FC } from "react"
import { useLogin } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import { LoginData } from "api/types"
import LoginForm from "./forms/LoginForm"

/**
 * Application login page
 */
const LoginPage: FC = () => {
  const loginMutation = useLogin()
  const { setCurrentUser } = useGlobalStore()

  const onSubmit = async (data: LoginData) => {
    try {
      await loginMutation.mutateAsync(data)
      setCurrentUser({ username: data.username })
    } catch {
      alert("Login failed")
    }
  }

  return <LoginForm onSubmit={onSubmit} />
}

export default LoginPage
