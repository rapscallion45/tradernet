import { FC, useState } from "react"
import { useForm } from "react-hook-form"
import { Button, Group, PasswordInput, Stack, Text, Title } from "@mantine/core"
import apiClient from "api/apiClient"

type ResetPasswordFormData = {
  password: string
  confirmPassword: string
}

type ResetPasswordFormProps = {
  username: string
  onReset: () => void
}

const ResetPasswordForm: FC<ResetPasswordFormProps> = ({ username, onReset }) => {
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
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

export default ResetPasswordForm
