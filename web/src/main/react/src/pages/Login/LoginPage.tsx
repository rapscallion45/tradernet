import { FC, ReactNode, useState } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { FormProvider, useForm } from "react-hook-form"
import { Box, Card, Center, Group, Stack, Text, Title } from "@mantine/core"
import { IconAlertTriangle, IconCircleLetterI } from "@tabler/icons-react"
import { useLogin } from "hooks/useAuth"
import { LoginData, LoginStatus } from "api/types"
import LoginForm from "./forms/LoginForm"
import ResetPasswordForm from "./forms/ResetPasswordForm"
import { QueryClientKeys } from "global/constants"

/**
 * Application login page
 */
const LoginPage: FC = () => {
  const loginMutation = useLogin()
  const loginForm = useForm<LoginData>({
    mode: "onSubmit",
    reValidateMode: "onChange",
  })
  const [loginStatus, setLoginStatus] = useState<LoginStatus | null>(null)
  const queryClient = useQueryClient()

  /**
   * Login form submission handler
   * @param username
   * @param password
   */
  const onSubmit = async ({ username, password }: LoginData) => {
    const response = await loginMutation.execute({ username, password })

    if (!response.ok) {
      setLoginStatus(LoginStatus.Unknown)
      return
    }
    setLoginStatus(response.data.status ?? LoginStatus.Unknown)

    if (response.data.status === LoginStatus.Success) {
      console.log("Login successful, invalidating cache...")
      await queryClient.invalidateQueries({ queryKey: [QueryClientKeys.Session] })
    }
  }

  return (
    <Center my={"xl"}>
      <Card padding={"lg"} w={420}>
        <Stack>
          {loginStatus !== LoginStatus.AccountPasswordExpired ? (
            <FormProvider {...loginForm}>
              <LoginForm onSubmit={onSubmit} />
              {loginStatus && <FeedbackArea status={loginStatus} />}
            </FormProvider>
          ) : (
            <ResetPasswordForm
              username={loginForm.getValues("username")}
              existingPassword={loginForm.getValues("password")}
              resetLoginStatus={() => {
                setLoginStatus(null)
                loginForm.resetField("password")
              }}
            />
          )}
        </Stack>
      </Card>
    </Center>
  )
}

export default LoginPage

/** local helper type for handling the login response status */
type Feedback = {
  icon: ReactNode
  summary: string
  message: string
}

/**
 * Maps login statuses to feedback messages, ensuring all statuses are handled.
 */
const statusMapping: { [K in LoginStatus]: Feedback } = {
  [LoginStatus.Success]: {
    icon: <IconCircleLetterI />,
    summary: "Login Succeeded",
    message: "Forwarding you to the application...",
  },
  [LoginStatus.AccountPasswordExpired]: {
    icon: <IconAlertTriangle color={"red"} />,
    summary: "Password Expired",
    message: "Your password has expired. Forwarding to Reset Password page...",
  },
  [LoginStatus.InvalidRequest]: {
    icon: <IconAlertTriangle color={"red"} />,
    summary: "Invalid Request",
    message: "The login request was malformed. Please try again or contact the System administrator.",
  },
  [LoginStatus.UserNotFound]: {
    icon: <IconAlertTriangle color={"red"} />,
    summary: "User Not Found",
    message: "A user with the given credentials was not found. Please try again or contact the System administrator.",
  },
  [LoginStatus.IncorrectCredentials]: {
    icon: <IconAlertTriangle color={"red"} />,
    summary: "Login Failed",
    message: "Username or Password incorrect. Please try again or contact the System administrator.",
  },
  [LoginStatus.Unknown]: {
    icon: <IconAlertTriangle color={"red"} size={15} />,
    summary: "Unknown Error",
    message: "An unknown error occurred when logging in. Please try again or contact the System administrator.",
  },
}

/**
 * Feedback area helper component for displaying messages sent back from login
 * @param status - current login status from server
 */
const FeedbackArea: FC<{ status: LoginStatus }> = ({ status }) => {
  const feedback = statusMapping[status]
  return (
    <Box role={"feedback"}>
      <Stack gap={"xs"}>
        <Title order={2} size={"h5"} fw={700} aria-live={"assertive"}>
          <Group gap={"sm"}>
            {feedback.icon} {feedback.summary}
          </Group>
        </Title>
        <Text fw={500} fz={"sm"} aria-live="polite">
          {feedback.message}
        </Text>
      </Stack>
    </Box>
  )
}
