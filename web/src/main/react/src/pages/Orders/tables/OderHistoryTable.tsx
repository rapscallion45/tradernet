import { FC, useMemo, useState } from "react"
import { Badge, Button, Text } from "@mantine/core"
import { ColumnDef } from "@tanstack/react-table"
import { OrderSummary } from "api/types"
import { ConfirmationModal } from "components/ConfirmationModal/ConfirmationModal"
import { Table } from "components/Table/Table"
import { useOrders } from "hooks/useOrders"
import { useCloseOrder } from "hooks/useCloseOrder"

const formatOrderDate = (value?: string): string => {
  if (!value) {
    return ""
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const dd = String(date.getDate()).padStart(2, "0")
  const mm = String(date.getMonth() + 1).padStart(2, "0")
  const yyyy = date.getFullYear()
  const hh = String(date.getHours()).padStart(2, "0")
  const min = String(date.getMinutes()).padStart(2, "0")
  const ss = String(date.getSeconds()).padStart(2, "0")

  return `${hh}:${min}:${ss} ${dd}/${mm}/${yyyy}`
}

/**
 * Table section for displaying order history and performance metrics.
 */
const OderHistoryTable: FC = () => {
  const { data: orders = [] } = useOrders()
  const closeOrder = useCloseOrder()
  const [pendingCloseOrder, setPendingCloseOrder] = useState<OrderSummary | null>(null)

  const columns = useMemo<ColumnDef<OrderSummary>[]>(
    () => [
      {
        accessorKey: "createdAt",
        header: "Created",
        cell: ({ row }) => formatOrderDate(row.original.createdAt),
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
        accessorKey: "aiPrediction",
        header: "AI Signal",
        cell: ({ row }) => {
          const aiPrediction = row.original.aiPrediction ?? "HOLD"
          const aiColor = aiPrediction === "BUY" ? "green" : aiPrediction === "SELL" ? "red" : "gray"
          return <Badge color={aiColor} variant={"filled"}>{aiPrediction}</Badge>
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
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => {
          const isClosed = row.original.status === "CLOSED"
          return (
            <Button
              size={"xs"}
              variant={"light"}
              disabled={isClosed}
              loading={closeOrder.isPending && closeOrder.variables === row.original.id}
              onClick={() => setPendingCloseOrder(row.original)}
            >
              {isClosed ? "Closed" : "Close"}
            </Button>
          )
        },
      },
    ],
    [closeOrder],
  )

  return (
    <>
      <Table<OrderSummary> columns={columns} data={orders} caption={orders.length === 0 ? "No orders yet." : undefined} />
      <ConfirmationModal
        title={"Close Trade"}
        opened={pendingCloseOrder !== null}
        onCancel={() => setPendingCloseOrder(null)}
        onConfirm={() => {
          if (!pendingCloseOrder) {
            return
          }
          closeOrder.mutate(pendingCloseOrder.id, {
            onSettled: () => setPendingCloseOrder(null),
          })
        }}
        loading={closeOrder.isPending}
        message={
          pendingCloseOrder
            ? `Are you sure you want to close ${pendingCloseOrder.side} ${pendingCloseOrder.quantity.toFixed(4)} ${pendingCloseOrder.symbol}?`
            : ""
        }
        confirmTextOverride={"Close Trade"}
      />
    </>
  )
}

export default OderHistoryTable
