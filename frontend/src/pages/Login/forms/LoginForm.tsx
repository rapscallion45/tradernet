import { FC } from "react"
import { useForm } from "react-hook-form"
import { Button, TextInput, Box } from "@mantine/core"
import { LoginData } from "api/types"

/**
 * Login Form props
 * @prop onSubmit - login submission callback handler
 */
type LoginFormProps = {
  onSubmit: (data: LoginData) => void
}

/**
 * Login Form component
 */
const LoginForm: FC<LoginFormProps> = ({ onSubmit }) => {
  const { register, handleSubmit } = useForm<LoginData>()

  return (
    <Box style={{ maxWidth: 300 }} mx={"auto"}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <TextInput label={"Username"} {...register("username")} required />
        <TextInput label={"Password"} type={"password"} {...register("password")} required />
        <Button type={"submit"} mt={"md"}>
          Login
        </Button>
      </form>
    </Box>
  )
}

export default LoginForm
