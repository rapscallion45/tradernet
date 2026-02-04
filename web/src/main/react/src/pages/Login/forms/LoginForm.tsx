import { FC } from "react"
import { useForm } from "react-hook-form"
import { Button, Center, Image, PasswordInput, Stack, Text, TextInput, Title } from "@mantine/core"
import { LoginData } from "api/types"
import TradernetLogo from "assets/tradernet-logo.svg"

/**
 * Login form component.
 */
type LoginFormProps = {
  onSubmit: (data: LoginData) => void
}

/**
 * Renders the login form and handles validation via react-hook-form.
 */
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
    <Stack align={"center"}>
      <Center>
        <Image src={TradernetLogo} alt={"Tradernet logo"} h={64} w={"auto"} />
      </Center>
      <Title order={3} ta={"center"}>
        Tradernet
      </Title>
      <Text size={"sm"} ta={"center"}>
        Enter your username and password to continue.
      </Text>
      <TextInput
        label={"Username"}
        data-testid={"username"}
        {...register("username", { required: "Required" })}
        error={errors.username?.message}
        autoComplete="username"
        w={"100%"}
      />
      <PasswordInput
        label={"Password"}
        data-testid={"password"}
        {...register("password", { required: "Required" })}
        error={errors.password?.message}
        autoComplete="current-password"
        w={"100%"}
      />
      <Button onClick={() => void handleSubmit(onSubmit)()} type={"submit"}>
        Log in
      </Button>
    </Stack>
  )
}

export default LoginForm
