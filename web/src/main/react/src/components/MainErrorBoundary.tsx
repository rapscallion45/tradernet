import { FC } from "react"
import { Box, Button, Container, Flex, Text, Title } from "@mantine/core"

/**
 * Highest error boundary in the application as a last fallback
 */
const MainErrorBoundary: FC = () => (
  <Container>
    <Flex direction={"column"} h={"100vh"} justify={"center"} align={"center"}>
      <Flex align={"center"} gap={"sm"}>
        <img src={"/logo.png"} alt={"Tradernet logo"} width={100} />
        <Title>Tradernet</Title>
      </Flex>
      <Text fw={600} mt={"md"}>
        An unexpected error occurred.
      </Text>
      <Box mt={"md"}>
        <Button onClick={() => window.location.reload()}>Retry</Button>
      </Box>
    </Flex>
  </Container>
)

export default MainErrorBoundary
