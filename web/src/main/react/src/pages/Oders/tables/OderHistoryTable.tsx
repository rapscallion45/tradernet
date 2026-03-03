import { FC, useMemo } from "react"
import { Badge, Text } from "@mantine/core"
import { ColumnDef } from "@tanstack/react-table"
import { OrderSummary } from "api/types"
import { Table } from "components/Table/Table"
import { useOrders } from "hooks/useOrders"

const OderHistoryTable: FC = () => {
  const { data: orders = [] } = useOrders()

  const columns = useMemo<ColumnDef<OrderSummary>[]>(
    () => [
      {
        accessorKey: "createdAtDisplay",
        header: "Created",
        cell: ({ row }) => row.original.createdAtDisplay ?? new Date(row.original.createdAt).toLocaleString(),
      },
      { accessorKey: "symbol", header: "Symbol" },
      { accessorKey: "side", header: "Side" },
      {
        accessorKey: "quantity",
        header: "Qty",
        cell: ({ row }) => row.original.quantity.toFixed(4),
      },
      {
        accessorKey: "price",
        header: "Entry",
        cell: ({ row }) => row.original.price.toFixed(4),
      },
      {
        accessorKey: "currentPriceDisplay",
        header: "Current",
        cell: ({ row }) => row.original.currentPriceDisplay ?? (row.original.currentPrice ?? row.original.price).toFixed(4),
      },
      {
        accessorKey: "pnlDisplay",
        header: "P/L",
        cell: ({ row }) => {
          const pnl = row.original.pnl ?? 0
          const pnlColor = pnl > 0 ? "green" : pnl < 0 ? "red" : "gray"
          return <Text c={pnlColor} fw={600}>{row.original.pnlDisplay ?? pnl.toFixed(4)}</Text>
        },
      },
      {
        accessorKey: "pnlPercentDisplay",
        header: "P/L %",
        cell: ({ row }) => {
          const pnlPercent = row.original.pnlPercent ?? 0
          const pnlColor = pnlPercent > 0 ? "green" : pnlPercent < 0 ? "red" : "gray"
          return <Text c={pnlColor} fw={600}>{row.original.pnlPercentDisplay ?? `${pnlPercent.toFixed(2)}%`}</Text>
        },
      },
      {
        accessorKey: "timing",
        header: "Timing",
        cell: ({ row }) => (
          <Badge color={row.original.timing === "GOOD" ? "green" : row.original.timing === "BAD" ? "red" : "gray"} variant={"light"}>
            {row.original.timing ?? "NEUTRAL"}
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

  return <Table<OrderSummary> columns={columns} data={orders} caption={orders.length === 0 ? "No orders yet." : undefined} />
}

export default OderHistoryTable
