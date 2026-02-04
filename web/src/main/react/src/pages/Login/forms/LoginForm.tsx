import { FC } from "react"
import { useForm } from "react-hook-form"
import { Button, PasswordInput, Stack, Text, TextInput } from "@mantine/core"
import { LoginData } from "api/types"

type LoginFormProps = {
  onSubmit: (data: LoginData) => void
}

const LoginForm: FC<LoginFormProps> = ({ onSubmit }) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginData>({
    mode: "onSubmit",
    reValidateMode: "onChange",
  })

  return (
    <Stack>
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
      <Button onClick={() => void handleSubmit(onSubmit)()} type={"submit"}>
        Log in
      </Button>
    </Stack>
  )
}

export default LoginForm
