import { FC, useMemo, useState } from "react"
import { Avatar, Badge, Button, Divider, Group, Modal, Select, Stack, Text, TextInput } from "@mantine/core"
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

const toDateInputValue = (value?: string): string => {
  if (!value) {
    return ""
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ""
  }

  const yyyy = String(date.getFullYear())
  const mm = String(date.getMonth() + 1).padStart(2, "0")
  const dd = String(date.getDate()).padStart(2, "0")
  return `${yyyy}-${mm}-${dd}`
}

/**
 * Table section for displaying order history and performance metrics.
 */
const OderHistoryTable: FC = () => {
  const { data: orders = [] } = useOrders()
  const closeOrder = useCloseOrder()
  const { currency } = useCurrencyPreference()
  const [pendingCloseOrder, setPendingCloseOrder] = useState<OrderSummary | null>(null)
  const [selectedOrder, setSelectedOrder] = useState<OrderSummary | null>(null)

  const [assetFilter, setAssetFilter] = useState("")
  const [positionFilter, setPositionFilter] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<string | null>(null)
  const [createdDateFilter, setCreatedDateFilter] = useState("")

  const filteredOrders = useMemo(() => {
    return orders.filter((order) => {
      if (assetFilter && !order.symbol.toLowerCase().includes(assetFilter.toLowerCase().trim())) {
        return false
      }

      if (positionFilter && order.side !== positionFilter) {
        return false
      }

      if (statusFilter && order.status !== statusFilter) {
        return false
      }

      if (createdDateFilter && toDateInputValue(order.createdAt) !== createdDateFilter) {
        return false
      }

      return true
    })
  }, [assetFilter, createdDateFilter, orders, positionFilter, statusFilter])

  const statusOptions = useMemo(() => [...new Set(orders.map((order) => order.status))], [orders])

  const orderById = useMemo(() => new Map(orders.map((order) => [order.id, order])), [orders])

  const columns = useMemo<ColumnDef<OrderSummary>[]>(
    () => [
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
              onClick={(event) => {
                event.stopPropagation()
                setPendingCloseOrder(row.original)
              }}>
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
      <Stack gap={"xs"} mb={"sm"}>
        <Group grow>
          <TextInput label={"Asset"} placeholder={"e.g. BTC"} value={assetFilter} onChange={(event) => setAssetFilter(event.currentTarget.value)} />
          <Select label={"Position"} placeholder={"All"} clearable data={["BUY", "SELL"]} value={positionFilter} onChange={setPositionFilter} />
          <Select label={"Status"} placeholder={"All"} clearable data={statusOptions} value={statusFilter} onChange={setStatusFilter} />
          <TextInput
            label={"Created date"}
            type={"date"}
            value={createdDateFilter}
            onChange={(event) => setCreatedDateFilter(event.currentTarget.value)}
          />
        </Group>
        <Group justify={"space-between"}>
          <Text size={"xs"} c={"dimmed"}>{`${filteredOrders.length} of ${orders.length} orders`}</Text>
          <Button
            size={"xs"}
            variant={"subtle"}
            onClick={() => {
              setAssetFilter("")
              setPositionFilter(null)
              setStatusFilter(null)
              setCreatedDateFilter("")
            }}>
            Clear filters
          </Button>
        </Group>
      </Stack>

      <Table<OrderSummary>
        columns={columns}
        data={filteredOrders}
        onRowClick={(id) => setSelectedOrder(orderById.get(id) ?? null)}
        caption={filteredOrders.length === 0 ? (orders.length === 0 ? "No orders yet." : "No orders match current filters.") : undefined}
        verticalSpacing={"xs"}
        horizontalSpacing={"xs"}
      />
      <Modal
        opened={selectedOrder !== null}
        onClose={() => setSelectedOrder(null)}
        title={selectedOrder ? `${selectedOrder.side === "SELL" ? "Sell" : "Buy"} Position` : "Position"}
        centered>
        {selectedOrder && (
          <Stack gap={8}>
            <Group justify={"space-between"} align={"center"} wrap={"nowrap"}>
              <Group gap={8} wrap={"nowrap"}>
                <Avatar src={getAssetLogoUrl(selectedOrder.symbol)} alt={selectedOrder.symbol} radius={"xl"} size={28}>
                  {getBaseAsset(selectedOrder.symbol).slice(0, 1)}
                </Avatar>
                <Stack gap={0}>
                  <Text size={"sm"} fw={700}>
                    {selectedOrder.symbol}
                  </Text>
                  <Text size={"xs"} c={"dimmed"}>{`${selectedOrder.side} · ${selectedOrder.status}`}</Text>
                </Stack>
              </Group>
              <Stack gap={0} align={"flex-end"}>
                <Text fw={700}>{formatCurrency(selectedOrder.currentPrice ?? selectedOrder.price, currency)}</Text>
                <Text fw={600} c={(selectedOrder.pnl ?? 0) > 0 ? "green" : (selectedOrder.pnl ?? 0) < 0 ? "red" : "gray"}>
                  {`${formatCurrency(selectedOrder.pnl ?? 0, currency)} (${(selectedOrder.pnlPercent ?? 0).toFixed(2)}%)`}
                </Text>
              </Stack>
            </Group>

            <Divider my={4} />
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Created</Text>
              <Text fw={600}>{formatDateTime(selectedOrder.createdAt)}</Text>
            </Group>
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Quantity</Text>
              <Text fw={600}>{formatNumber(selectedOrder.quantity, { minimumFractionDigits: 4, maximumFractionDigits: 4 })}</Text>
            </Group>
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Entry</Text>
              <Text fw={600}>{formatCurrency(selectedOrder.price, currency)}</Text>
            </Group>
          </Stack>
        )}
      </Modal>
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
