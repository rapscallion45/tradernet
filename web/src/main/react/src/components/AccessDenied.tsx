import { FC } from "react"
import { Box, Button, Container, Flex, Text, Title } from "@mantine/core"
import { useNavigate } from "react-router-dom"
import Routes from "global/Routes"

/**
 * Access Denied props
 * @prop height - intended height for the message component
 */
type AccessDeniedProps = {
  height?: number | string
}

/**
 * Access Denied component for displaying no access message
 */
const AccessDenied: FC<AccessDeniedProps> = ({ height = "calc(100vh - 120px)" }) => {
  const navigate = useNavigate()

  return (
    <Container>
      <Flex direction={"column"} h={height} justify={"center"} align={"center"}>
        <Flex align={"center"} gap={"sm"}>
          <img src={"/logo.png"} alt={"Tradernet logo"} width={100} />
          <Title>Tradernet</Title>
        </Flex>
        <Text fw={600} mt={"md"}>
          The currently logged in user does not have permission to access the requested resource.
        </Text>
        <Box mt={"md"}>
          <Button onClick={() => navigate(Routes.Dashboard)}>Go Back</Button>
        </Box>
      </Flex>
    </Container>
  )
}

export default AccessDenied
