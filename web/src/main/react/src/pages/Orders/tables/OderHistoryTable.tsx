import { FC, useMemo, useState } from "react"
import { Avatar, Badge, Button, Group, Stack, Text } from "@mantine/core"
import { ColumnDef } from "@tanstack/react-table"
import { OrderSummary } from "api/types"
import { ConfirmationModal } from "components/ConfirmationModal/ConfirmationModal"
import { Table } from "components/Table/Table"
import { useCloseOrder } from "hooks/useCloseOrder"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"
import { useOrders } from "hooks/useOrders"
import { formatCurrency, formatDateTime, formatNumber } from "utils/intl"

const QUOTE_SUFFIXES = ["USDT", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "BRL", "TRY", "BTC", "ETH", "BNB"]

const getBaseAsset = (symbol: string): string => {
  const upper = symbol.toUpperCase()
  const matchedQuote = QUOTE_SUFFIXES.find((quote) => upper.endsWith(quote))
  return matchedQuote ? upper.slice(0, upper.length - matchedQuote.length) : upper
}

const getAssetLogoUrl = (symbol: string): string => {
  const baseAsset = getBaseAsset(symbol).toLowerCase()
  return `https://cdn.jsdelivr.net/gh/spothq/cryptocurrency-icons@master/128/color/${baseAsset}.png`
}

/**
 * Table section for displaying order history and performance metrics.
 */
const OderHistoryTable: FC = () => {
  const { data: orders = [] } = useOrders()
  const closeOrder = useCloseOrder()
  const { currency } = useCurrencyPreference()
  const [pendingCloseOrder, setPendingCloseOrder] = useState<OrderSummary | null>(null)

  const columns = useMemo<ColumnDef<OrderSummary>[]>(
    () => [
      {
        accessorKey: "createdAt",
        header: "Created",
        cell: ({ row }) => formatDateTime(row.original.createdAt),
      },
      {
        accessorKey: "symbol",
        header: "Asset",
        cell: ({ row }) => {
          const symbol = row.original.symbol
          const baseAsset = getBaseAsset(symbol)
          const currentValue = row.original.currentPrice ?? row.original.price

          return (
            <Group gap={8} wrap={"nowrap"}>
              <Avatar src={getAssetLogoUrl(symbol)} alt={symbol} radius={"xl"} size={24}>
                {baseAsset.slice(0, 1)}
              </Avatar>
              <Stack gap={0}>
                <Text size={"sm"} fw={600}>
                  {symbol}
                </Text>
                <Text size={"xs"} c={"dimmed"}>
                  {formatCurrency(currentValue, currency)}
                </Text>
              </Stack>
            </Group>
          )
        },
      },
      { accessorKey: "side", header: "Position" },
      {
        accessorKey: "quantity",
        header: "Qty",
        cell: ({ row }) => formatNumber(row.original.quantity, { minimumFractionDigits: 4, maximumFractionDigits: 4 }),
      },
      {
        accessorKey: "price",
        header: "Entry",
        cell: ({ row }) => formatCurrency(row.original.price, currency),
      },
      {
        accessorKey: "pnlDisplay",
        header: "P/L",
        cell: ({ row }) => {
          const pnl = row.original.pnl ?? 0
          const pnlColor = pnl > 0 ? "green" : pnl < 0 ? "red" : "gray"
          return (
            <Text c={pnlColor} fw={600}>
              {formatCurrency(pnl, currency)}
            </Text>
          )
        },
      },
      {
        accessorKey: "pnlPercentDisplay",
        header: "Change",
        cell: ({ row }) => {
          const pnlPercent = row.original.pnlPercent ?? 0
          const pnlColor = pnlPercent > 0 ? "green" : pnlPercent < 0 ? "red" : "gray"
          return (
            <Text c={pnlColor} fw={600}>
              {`${pnlPercent.toFixed(2)}%`}
            </Text>
          )
        },
      },
      {
        accessorKey: "aiPrediction",
        header: "AI Signal",
        cell: ({ row }) => {
          const aiPrediction = row.original.aiPrediction ?? "HOLD"
          const aiColor = aiPrediction === "BUY" ? "green" : aiPrediction === "SELL" ? "red" : "gray"
          return (
            <Badge color={aiColor} variant={"filled"}>
              {aiPrediction}
            </Badge>
          )
        },
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
              onClick={() => setPendingCloseOrder(row.original)}>
              {isClosed ? "Closed" : "Close"}
            </Button>
          )
        },
      },
    ],
    [closeOrder, currency],
  )

  return (
    <>
      <Table<OrderSummary>
        columns={columns}
        data={orders}
        caption={orders.length === 0 ? "No orders yet." : undefined}
        verticalSpacing={"xs"}
        horizontalSpacing={"xs"}
      />
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
            ? `Are you sure you want to close ${pendingCloseOrder.side} ${formatNumber(pendingCloseOrder.quantity, { minimumFractionDigits: 4, maximumFractionDigits: 4 })} ${pendingCloseOrder.symbol}?`
            : ""
        }
        confirmTextOverride={"Close Trade"}
      />
    </>
  )
}

export default OderHistoryTable
