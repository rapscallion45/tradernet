import { FC, useState } from "react"
import { Button, Card, Center, Group, PasswordInput, Stack, Text, Title } from "@mantine/core"
import { useNavigate } from "react-router-dom"
import Routes from "global/Routes"

/**
 * Password reset page shown when the default admin password is used.
 */
const ResetPasswordPage: FC = () => {
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const navigate = useNavigate()

  const handleSubmit = () => {
    if (newPassword.length === 0 || confirmPassword.length === 0) {
      alert("Please enter and confirm a new password.")
      return
    }

    if (newPassword !== confirmPassword) {
      alert("Passwords do not match.")
      return
    }

    alert("Password reset submitted. Please log in again with your new password once the backend supports this action.")
    navigate(Routes.Login)
  }

  return (
    <Center my={"xl"}>
      <Card padding={"lg"} w={420}>
        <Stack>
          <Title order={3}>Reset Admin Password</Title>
          <Text size={"sm"}>
            The default admin password was used. Please set a new password before continuing.
          </Text>
          <PasswordInput
            label={"New password"}
            value={newPassword}
            onChange={(event) => setNewPassword(event.currentTarget.value)}
            required
          />
          <PasswordInput
            label={"Confirm password"}
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.currentTarget.value)}
            required
          />
          <Group justify={"flex-end"}>
            <Button onClick={handleSubmit}>Submit</Button>
          </Group>
        </Stack>
      </Card>
    </Center>
  )
}

export default ResetPasswordPage
