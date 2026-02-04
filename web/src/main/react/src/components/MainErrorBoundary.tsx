import { FC } from "react"
import { Badge, Card, Group, Stack, Text, Title } from "@mantine/core"
import logo from "assets/tradernet-logo.svg"
import CenterOnPage from "components/CenterOnPage"
import Button from "components/Button/Button"

/**
 * Main Error Boundary props
 * @prop title - title of the error
 * @prop message - error message string
 */
type MainErrorBoundaryProps = {
  title?: string
  message?: string
}

/**
 * Highest error boundary in the application as a last fallback
 */
const MainErrorBoundary: FC<MainErrorBoundaryProps> = ({ title, message }) => (
  <CenterOnPage>
    <Card p="lg">
      <Stack>
        <Group justify={"center"}>
          <img src={logo} alt={"Tradernet logo"} width={50} />
          <Title fw={400}>Tradernet</Title>
        </Group>
        <Group justify={"center"}>
          <Title order={4} ta={"center"}>
            {title ?? "Error"}
          </Title>
          <Badge color="red">ERROR</Badge>
        </Group>
        <Text fw={600} ta={"center"}>
          {message ?? "An unexpected error occurred."}
        </Text>
        <Button onClick={() => window.location.reload()}>Retry</Button>
      </Stack>
    </Card>
  </CenterOnPage>
)

export default MainErrorBoundary
