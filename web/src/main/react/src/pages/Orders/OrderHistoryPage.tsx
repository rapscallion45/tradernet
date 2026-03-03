import { FC, Suspense, useMemo } from "react"
import { Badge, Stack, Text } from "@mantine/core"
import { ColumnDef } from "@tanstack/react-table"
import { Table } from "components/Table/Table"
import { Title } from "components/Title/Title"
import PageLoadingSkeleton from "components/PageLoadingSkeleton"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { useOrders } from "hooks/useOrders"

type OrderHistoryRow = {
  id: string
  created: string
  symbol: string
  side: string
  quantity: string
  entry: string
  current: string
  pnl: number
  pnlText: string
  pnlPercent: number
  pnlPercentText: string
  timing: string
  status: string
}

const OrderHistoryTable: FC = () => {
  const { data: orders = [] } = useOrders()

  const rows = useMemo<OrderHistoryRow[]>(() => {
    return orders.map((order) => {
      const pnl = order.pnl ?? 0
      const pnlPercent = order.pnlPercent ?? 0

      return {
        id: `${order.orderId}-${order.createdAt}`,
        created: new Date(order.createdAt).toLocaleString(),
        symbol: order.symbol,
        side: order.side,
        quantity: order.quantity.toFixed(4),
        entry: order.price.toFixed(4),
        current: (order.currentPrice ?? order.price).toFixed(4),
        pnl,
        pnlText: pnl.toFixed(4),
        pnlPercent,
        pnlPercentText: `${pnlPercent.toFixed(2)}%`,
        timing: order.timing ?? "NEUTRAL",
        status: order.status,
      }
    })
  }, [orders])

  const columns = useMemo<ColumnDef<OrderHistoryRow>[]>(
    () => [
      { accessorKey: "created", header: "Created" },
      { accessorKey: "symbol", header: "Symbol" },
      { accessorKey: "side", header: "Side" },
      { accessorKey: "quantity", header: "Qty" },
      { accessorKey: "entry", header: "Entry" },
      { accessorKey: "current", header: "Current" },
      {
        accessorKey: "pnlText",
        header: "P/L",
        cell: ({ row }) => {
          const pnl = row.original.pnl
          const pnlColor = pnl > 0 ? "green" : pnl < 0 ? "red" : "gray"
          return (
            <Text c={pnlColor} fw={600}>
              {row.original.pnlText}
            </Text>
          )
        },
      },
      {
        accessorKey: "pnlPercentText",
        header: "P/L %",
        cell: ({ row }) => {
          const pnlPercent = row.original.pnlPercent
          const pnlColor = pnlPercent > 0 ? "green" : pnlPercent < 0 ? "red" : "gray"
          return (
            <Text c={pnlColor} fw={600}>
              {row.original.pnlPercentText}
            </Text>
          )
        },
      },
      {
        accessorKey: "timing",
        header: "Timing",
        cell: ({ row }) => (
          <Badge color={row.original.timing === "GOOD" ? "green" : row.original.timing === "BAD" ? "red" : "gray"} variant={"light"}>
            {row.original.timing}
          </Badge>
        ),
      },
      {
        accessorKey: "status",
        header: "Status",
        cell: ({ row }) => <Badge variant={"outline"}>{row.original.status}</Badge>,
      },
    ],
    [],
  )

  return <Table<OrderHistoryRow> columns={columns} data={rows} caption={rows.length === 0 ? "No orders yet." : undefined} />
}

const OrderHistoryPage: FC = () => {
  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Order History</Title>} description={"Track your orders and their current P/L performance."} />

      <Stack>
        <SectionHeading>ORDERS</SectionHeading>
        <Suspense fallback={<PageLoadingSkeleton />}>
          <OrderHistoryTable />
        </Suspense>
      </Stack>
    </Stack>
  )
}

export default OrderHistoryPage
