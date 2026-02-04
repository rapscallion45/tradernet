import { FC, useMemo, useState } from "react"
import { useNavigate } from "react-router-dom"
import { Card, Center, Stack, Text, Title } from "@mantine/core"
import apiClient from "api/apiClient"
import { useLogin } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import { AuthSessionResponse, LoginData, LoginStatus } from "api/types"
import Routes from "global/Routes"
import LoginForm from "./forms/LoginForm"
import ResetPasswordForm from "./forms/ResetPasswordForm"

/**
 * Application login page
 */
const LoginPage: FC = () => {
  const loginMutation = useLogin()
  const { setCurrentUser } = useGlobalStore()
  const navigate = useNavigate()
  const [loginStatus, setLoginStatus] = useState<LoginStatus | null>(null)
  const [loginCredentials, setLoginCredentials] = useState<LoginData | null>(null)
  const onSubmit = async (data: LoginData) => {
    try {
      const response = await loginMutation.mutateAsync(data)
      const status = response.data.status
      setLoginStatus(status)

      if (status === LoginStatus.AccountPasswordExpired) {
        setLoginCredentials(data)
        return
      }

      if (status === LoginStatus.Success) {
        const session = await apiClient.get<AuthSessionResponse>("/auth/session")
        setCurrentUser(session.data)
        navigate(Routes.Dashboard)
      }
    } catch {
      setLoginStatus(LoginStatus.Unknown)
    }
  }

  const shouldShowResetForm = loginStatus === LoginStatus.AccountPasswordExpired
  const loginFeedback = useMemo(() => (loginStatus ? statusMapping[loginStatus] : null), [loginStatus])

  return (
    <Center my={"xl"}>
      <Card padding={"lg"} w={420}>
        <Stack>
          {!shouldShowResetForm ? (
            <>
              <LoginForm onSubmit={onSubmit} />
              {loginFeedback && <FeedbackArea title={loginFeedback.title} message={loginFeedback.message} />}
            </>
          ) : (
            <ResetPasswordForm
              username={loginCredentials?.username ?? ""}
              onReset={() => {
                setLoginStatus(null)
                setLoginCredentials(null)
              }}
            />
          )}
        </Stack>
      </Card>
    </Center>
  )
}

export default LoginPage

const statusMapping: Record<LoginStatus, { title: string; message: string }> = {
  [LoginStatus.Success]: {
    title: "Login succeeded",
    message: "Forwarding you to the application...",
  },
  [LoginStatus.IncorrectCredentials]: {
    title: "Login failed",
    message: "Username or password incorrect. Please try again.",
  },
  [LoginStatus.UserNotFound]: {
    title: "User not found",
    message: "This account does not exist. Please contact an administrator.",
  },
  [LoginStatus.InvalidRequest]: {
    title: "Missing credentials",
    message: "Please enter both a username and password.",
  },
  [LoginStatus.AccountPasswordExpired]: {
    title: "Password expired",
    message: "Please set a new password before continuing.",
  },
  [LoginStatus.Unknown]: {
    title: "Unknown error",
    message: "An unknown error occurred while logging in. Please try again.",
  },
}

const FeedbackArea: FC<{ title: string; message: string }> = ({ title, message }) => (
  <Stack gap={"xs"} role={"status"}>
    <Title order={4}>{title}</Title>
    <Text size={"sm"}>{message}</Text>
  </Stack>
)
