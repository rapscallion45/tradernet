import { FC } from "react"
import { useNavigate } from "react-router-dom"
import { Stack, TextInput } from "@mantine/core"
import { spotlight } from "@mantine/spotlight"
import useSession from "hooks/useSession"
import useHealthCheck from "hooks/useHealthCheck"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { Title } from "components/Title/Title"
import { ActionCard } from "components/ActionCard/ActionCard"
import { CardGrid } from "components/CardGrid/CardGrid"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import Routes from "global/Routes"
import { IconArrowRight, IconReceipt } from "@tabler/icons-react"
import { useOrders } from "hooks/useOrders"
import { StatCard } from "components/StatCard/StatCard"
import { OrderCard } from "components/OrderCard/OrderCard"
import { TradingChartPanel } from "components/TradingChartPanel/TradingChartPanel"

/**
 * Application Dashboard page
 */
const DashboardPage: FC = () => {
  const navigate = useNavigate()
  const { data: session } = useSession()
  const { data: health } = useHealthCheck()
  const { data: orders = [] } = useOrders()

  return (
    <Stack gap={"xl"}>
      <PageHeader
        title={<Title highlight={session.username}>{`Welcome ${session.username}, what would you like to do today?`}</Title>}
        description={`Tradernet server is ${health.status}.`}
        rightSection={[
          <TextInput
            placeholder={"Search (Ctrl + K)"}
            onClick={spotlight.open}
            onKeyDown={(e) => {
              e.preventDefault()
              spotlight.open()
            }}
            size={"md"}
            variant={"filled"}
            miw={150}
            aria-label={"Search"}
          />,
        ]}
      />
      <Stack gap={"lg"}>
        <Stack>
          <SectionHeading>MARKET CHART</SectionHeading>
          <TradingChartPanel />
        </Stack>

        <Stack>
          <SectionHeading>ACTIONS</SectionHeading>
          <CardGrid>
            <OrderCard />
          </CardGrid>
        </Stack>

        <Stack>
          <SectionHeading>ORDER HISTORY</SectionHeading>
          <CardGrid>
            <StatCard text={String(orders.length)} secondaryText={"Orders Placed"} icon={<IconReceipt />} />
            <ActionCard
              text={"View Orders"}
              icon={<IconArrowRight />}
              secondaryText={"View your order book history"}
              onClick={() => navigate(Routes.Orders)}
            />
          </CardGrid>
        </Stack>
      </Stack>
    </Stack>
  )
}

export default DashboardPage
