import { FC } from "react"
import { useForm } from "react-hook-form"
import { Center, Group, Image, PasswordInput, Stack, Text, Title } from "@mantine/core"
import TradernetLogo from "assets/tradernet-logo.svg"
import { Button } from "components/Button/Button"
import { useToast } from "hooks/useToast"
import { validateFieldMatches } from "utils/forms"
import { getRestClient } from "api/RestClient"
import { getErrorMessage } from "api/util"

/**
 * Reset-password form field data.
 */
type ResetPasswordFormData = {
  password: string
  confirmPassword: string
}

/**
 * Reset-password form props.
 */
type ResetPasswordFormProps = {
  resetLoginStatus: () => void
}

/**
 * Change password form that is shown when LoginStatus is AccountPasswordExpired.
 * The backend issues a short-lived, HTTP-only reset cookie after validating the expired-password login.
 */
const ResetPasswordForm: FC<ResetPasswordFormProps> = ({ resetLoginStatus }) => {
  const { toast } = useToast()
  const {
    register,
    formState: { errors },
    handleSubmit,
    getValues,
  } = useForm<ResetPasswordFormData>({
    mode: "onBlur",
    reValidateMode: "onChange",
  })

  const onSubmit = handleSubmit(
    async ({ password, confirmPassword }) => {
      if (password !== confirmPassword) throw new Error("Passwords do not match! This is a fatal error and should have been caught in the form validation.")
      try {
        await getRestClient().authResource.forgotPassword({ newPassword: password })
        toast({
          id: "password-change",
          title: "Password changed successfully",
          message: "You can now log in with your new password.",
          variant: "success",
          timestamp: Date.now(),
        })
        resetLoginStatus()
      } catch (error: unknown) {
        toast({
          id: "password-change",
          title: "Password change failed",
          message: getErrorMessage(error),
          variant: "error",
          timestamp: Date.now(),
        })
      }
    },
    (errors) => {
      console.log("Password change failed!", errors)
      toast({
        id: "password-change",
        title: "Password change failed!",
        message: "Please try again or contact an administrator",
        variant: "error",
        timestamp: Date.now(),
      })
    },
  )

  return (
    <Stack>
      <Center>
        <Image src={TradernetLogo} alt={"Tradernet logo"} h={100} w={"auto"} />
      </Center>
      <Title order={2} ta={"center"}>
        Password Expired
      </Title>
      <Text ta={"center"}>Please enter a new password to access the system.</Text>
      <PasswordInput
        label={"Password"}
        data-testid={"password"}
        {...register("password", {
          required: "Required",
          deps: ["confirmPassword"],
        })}
        error={errors.password?.message}
        aria-label={"Password field"}
        autoComplete={"new-password"}
      />
      <PasswordInput
        label={"Confirm Password"}
        data-testid={"confirmPassword"}
        {...register("confirmPassword", {
          required: "Required",
          validate: { validatePassword: validateFieldMatches<ResetPasswordFormData>("password", getValues) },
        })}
        error={errors.confirmPassword?.message}
        aria-label={"Confirm Password field"}
        autoComplete={"confirm-password"}
      />
      <Group justify={"flex-end"}>
        <Button onClick={() => void onSubmit()} size={"lg"} variant={"filled"}>
          Change Password
        </Button>
      </Group>
    </Stack>
  )
}

export default ResetPasswordForm
