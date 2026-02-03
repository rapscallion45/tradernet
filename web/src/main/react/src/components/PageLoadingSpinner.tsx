import { FC } from "react"
import { Container, Flex, Loader, Text, Title } from "@mantine/core"

/**
 * Page Loading Spinner props
 * @prop message - message to be displayed under spinner
 */
type PageLoadingSpinnerProps = {
  message?: string
}

/**
 * Page Loading spinner component
 */
const PageLoadingSpinner: FC<PageLoadingSpinnerProps> = ({ message }) => (
  <Container>
    <Flex direction={"column"} h={"100vh"} justify={"center"} align={"center"} gap={"md"}>
      <Flex align={"center"} gap={"sm"}>
        <img src={"/logo.png"} alt={"Tradernet logo"} width={100} />
        <Title>Tradernet</Title>
      </Flex>
      <Loader />
      <Text fw={600} mih={30}>
        {message}
      </Text>
    </Flex>
    mpipe
  </Container>
)

export default PageLoadingSpinner
