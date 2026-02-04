import { FC } from "react"
import { Card, Group, Loader, Stack, Text, Title } from "@mantine/core"
import CenterOnPage from "components/CenterOnPage"
import logo from "assets/tradernet-logo.svg"

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
  <CenterOnPage>
    <Card p="lg">
      <Stack align={"center"}>
        <Group justify={"center"}>
          <img src={logo} alt={"Tradernet logo"} width={50} />
          <Title fw={400}>Tradernet</Title>
        </Group>
        <Loader color={"secondary"} />
        <Text fw={600} ta={"center"}>
          {message ?? "Initialising..."}
        </Text>
      </Stack>
    </Card>
  </CenterOnPage>
)

export default PageLoadingSpinner
