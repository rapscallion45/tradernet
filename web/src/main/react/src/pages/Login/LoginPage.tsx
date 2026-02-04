import { FC, useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { Button, Card, Center, Group, PasswordInput, Stack, Text, TextInput, Title } from "@mantine/core"
import apiClient from "api/apiClient"
import { useLogin } from "hooks/useAuth"
import { useGlobalStore } from "hooks/useGlobalStore"
import { AuthSessionResponse, LoginData, LoginStatus } from "api/types"
import Routes from "global/Routes"

/**
 * Application login page
 */
const LoginPage: FC = () => {
  const loginMutation = useLogin()
  const { setCurrentUser } = useGlobalStore()
  const navigate = useNavigate()
  const [loginStatus, setLoginStatus] = useState<LoginStatus | null>(null)
  const [loginCredentials, setLoginCredentials] = useState<LoginData | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
    resetField,
  } = useForm<LoginData>({
    mode: "onSubmit",
    reValidateMode: "onChange",
  })

  const onSubmit = handleSubmit(async (data) => {
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
  })

  const shouldShowResetForm = loginStatus === LoginStatus.AccountPasswordExpired
  const loginFeedback = useMemo(() => (loginStatus ? statusMapping[loginStatus] : null), [loginStatus])

  return (
    <Center my={"xl"}>
      <Card padding={"lg"} w={420}>
        <Stack>
          <Title order={3}>Welcome to Tradernet</Title>
          {!shouldShowResetForm ? (
            <>
              <Text size={"sm"}>Enter your username and password to continue.</Text>
              <TextInput
                label={"Username"}
                data-testid={"username"}
                {...register("username", { required: "Required" })}
                error={errors.username?.message}
                autoComplete="username"
              />
              <PasswordInput
                label={"Password"}
                data-testid={"password"}
                {...register("password", { required: "Required" })}
                error={errors.password?.message}
                autoComplete="current-password"
              />
              <Button onClick={() => void onSubmit()} type={"submit"}>
                Log in
              </Button>
              {loginFeedback && <FeedbackArea title={loginFeedback.title} message={loginFeedback.message} />}
            </>
          ) : (
            <ChangePasswordForm
              username={loginCredentials?.username ?? getValues("username")}
              onReset={() => {
                setLoginStatus(null)
                setLoginCredentials(null)
                resetField("password")
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

type ChangePasswordFormData = {
  password: string
  confirmPassword: string
}

type ChangePasswordFormProps = {
  username: string
  onReset: () => void
}

const ChangePasswordForm: FC<ChangePasswordFormProps> = ({ username, onReset }) => {
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<ChangePasswordFormData>({
    mode: "onBlur",
    reValidateMode: "onChange",
  })
  const [submitStatus, setSubmitStatus] = useState<"idle" | "error" | "success">("idle")

  const onSubmitPassword = handleSubmit(async ({ password, confirmPassword }) => {
    if (password !== confirmPassword) {
      setSubmitStatus("error")
      return
    }

    try {
      await apiClient.post("/auth/forgot-password", { username, newPassword: password })
      setSubmitStatus("success")
      onReset()
    } catch {
      setSubmitStatus("error")
    }
  })

  return (
    <Stack>
      <Title order={4}>Password Expired</Title>
      <Text size={"sm"}>Please enter a new password to access the system.</Text>
      <PasswordInput
        label={"New password"}
        data-testid={"new-password"}
        {...register("password", { required: "Required" })}
        error={errors.password?.message}
        autoComplete="new-password"
      />
      <PasswordInput
        label={"Confirm password"}
        data-testid={"confirm-password"}
        {...register("confirmPassword", {
          required: "Required",
          validate: (value) => value === getValues("password") || "Passwords do not match",
        })}
        error={errors.confirmPassword?.message}
        autoComplete="new-password"
      />
      <Group justify={"flex-end"}>
        <Button onClick={() => void onSubmitPassword()} size={"md"} variant={"filled"}>
          Change Password
        </Button>
      </Group>
      {submitStatus === "error" && (
        <Text size={"sm"} c={"red"}>
          Password change failed. Please try again.
        </Text>
      )}
      {submitStatus === "success" && (
        <Text size={"sm"} c={"green"}>
          Password updated. Please log in with your new credentials.
        </Text>
      )}
    </Stack>
  )
}

const FeedbackArea: FC<{ title: string; message: string }> = ({ title, message }) => (
  <Stack gap={"xs"} role={"status"}>
    <Title order={4}>{title}</Title>
    <Text size={"sm"}>{message}</Text>
  </Stack>
)
