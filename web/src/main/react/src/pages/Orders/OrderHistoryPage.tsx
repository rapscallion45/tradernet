import { FC, Suspense } from "react"
import { Stack } from "@mantine/core"
import { Title } from "components/Title/Title"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import OderHistoryTable from "pages/Orders/tables/OderHistoryTable"

/**
 * Page for viewing historical orders and their current performance.
 */
const OrderHistoryPage: FC = () => {
  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Order History</Title>} description={"Track your orders and their current P/L performance."} />

      <Stack>
        <SectionHeading>ALL ORDERS</SectionHeading>
        <Suspense fallback={<PageLoadingSkeleton />}>
          <OderHistoryTable />
        </Suspense>
      </Stack>
    </Stack>
  )
}

export default OrderHistoryPage
