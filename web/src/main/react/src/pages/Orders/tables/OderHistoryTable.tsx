import { FC, useMemo, useState } from "react"
import { ActionIcon, Avatar, Badge, Button, Collapse, Divider, Group, Select, SimpleGrid, Stack, Text, TextInput } from "@mantine/core"
import { IconFilter, IconFilterOff } from "@tabler/icons-react"
import { ColumnDef } from "@tanstack/react-table"
import { OrderSummary } from "api/types"
import { ConfirmationModal } from "components/ConfirmationModal/ConfirmationModal"
import { Table } from "components/Table/Table"
import { useCloseOrder } from "hooks/useCloseOrder"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"
import { useOrders } from "hooks/useOrders"
import { formatCurrency, formatDateTime, formatNumber } from "utils/intl"
import { getAssetLogoUrl, getBaseAsset } from "utils/marketAssets"

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
  const [createdDateFromFilter, setCreatedDateFromFilter] = useState("")
  const [createdDateToFilter, setCreatedDateToFilter] = useState("")
  const [filtersExpanded, setFiltersExpanded] = useState(true)
  const hasFiltersApplied = Boolean(assetFilter || positionFilter || statusFilter || createdDateFromFilter || createdDateToFilter)

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

      const createdDate = toDateInputValue(order.createdAt)
      if (createdDateFromFilter && createdDate < createdDateFromFilter) {
        return false
      }

      if (createdDateToFilter && createdDate > createdDateToFilter) {
        return false
      }

      return true
    })
  }, [assetFilter, createdDateFromFilter, createdDateToFilter, orders, positionFilter, statusFilter])

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
        accessorKey: "netValue",
        header: "Net Value",
        cell: ({ row }) => formatCurrency(row.original.netValue ?? row.original.price * row.original.quantity + (row.original.pnl ?? 0), currency),
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
        <Group justify={"space-between"}>
          <Group gap={"xs"}>
            <Text size={"sm"} fw={600}>
              {hasFiltersApplied ? `Filtered Orders (${filteredOrders.length})` : `All Orders (${orders.length})`}
            </Text>
            <ActionIcon
              size={"sm"}
              variant={"subtle"}
              aria-label={filtersExpanded ? "Hide filters" : "Show filters"}
              onClick={() => setFiltersExpanded((current) => !current)}>
              {filtersExpanded ? <IconFilterOff size={16} /> : <IconFilter size={16} />}
            </ActionIcon>
          </Group>
          <Button
            size={"xs"}
            variant={"subtle"}
            onClick={() => {
              setAssetFilter("")
              setPositionFilter(null)
              setStatusFilter(null)
              setCreatedDateFromFilter("")
              setCreatedDateToFilter("")
            }}>
            Clear filters
          </Button>
        </Group>
        <Collapse in={filtersExpanded}>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 5 }} spacing={"sm"}>
            <TextInput label={"Asset"} placeholder={"e.g. BTC"} value={assetFilter} onChange={(event) => setAssetFilter(event.currentTarget.value)} />
            <Select label={"Position"} placeholder={"All"} clearable data={["BUY", "SELL"]} value={positionFilter} onChange={setPositionFilter} />
            <Select label={"Status"} placeholder={"All"} clearable data={statusOptions} value={statusFilter} onChange={setStatusFilter} />
            <TextInput
              label={"Created from"}
              type={"date"}
              value={createdDateFromFilter}
              onChange={(event) => setCreatedDateFromFilter(event.currentTarget.value)}
            />
            <TextInput label={"Created to"} type={"date"} value={createdDateToFilter} onChange={(event) => setCreatedDateToFilter(event.currentTarget.value)} />
          </SimpleGrid>
        </Collapse>
      </Stack>

      <Table<OrderSummary>
        columns={columns}
        data={filteredOrders}
        onRowClick={(id) => setSelectedOrder(orderById.get(id) ?? null)}
        caption={filteredOrders.length === 0 ? (orders.length === 0 ? "No orders yet." : "No orders match current filters.") : undefined}
        verticalSpacing={"xs"}
        horizontalSpacing={"xs"}
      />
      <ConfirmationModal
        opened={selectedOrder !== null}
        onCancel={() => setSelectedOrder(null)}
        onConfirm={() => {
          if (!selectedOrder) {
            return
          }
          setPendingCloseOrder(selectedOrder)
          setSelectedOrder(null)
        }}
        disableConfirm={selectedOrder?.status === "CLOSED"}
        title={selectedOrder ? `${selectedOrder.side === "SELL" ? "Sell" : "Buy"} Position` : "Position"}
        confirmTextOverride={"Close Trade"}>
        {selectedOrder && (
          <Stack gap={8}>
            <Group gap={8} wrap={"nowrap"}>
              <Avatar src={getAssetLogoUrl(selectedOrder.symbol)} alt={selectedOrder.symbol} radius={"xl"} size={63}>
                {getBaseAsset(selectedOrder.symbol).slice(0, 1)}
              </Avatar>
              <Stack gap={0}>
                <Text size={"lg"} fw={700}>
                  {selectedOrder.symbol}
                </Text>
                <Group gap={8} align={"center"} wrap={"nowrap"}>
                  <Text size={"md"} c={"dimmed"}>
                    {formatCurrency(selectedOrder.currentPrice ?? selectedOrder.price, currency)}
                  </Text>
                  <Text size={"xs"} fw={600} c={(selectedOrder.pnl ?? 0) > 0 ? "green" : (selectedOrder.pnl ?? 0) < 0 ? "red" : "gray"}>
                    {`${formatCurrency(selectedOrder.pnl ?? 0, currency)} (${(selectedOrder.pnlPercent ?? 0).toFixed(2)}%)`}
                  </Text>
                </Group>
              </Stack>
            </Group>

            <Text fz={"0.66rem"} c={"dimmed"}>{`Trade opened on: ${formatDateTime(selectedOrder.createdAt)}`}</Text>

            <Divider my={4} />
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Quantity</Text>
              <Text fw={600}>{formatNumber(selectedOrder.quantity, { minimumFractionDigits: 4, maximumFractionDigits: 4 })}</Text>
            </Group>
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Entry Price</Text>
              <Text fw={600}>{formatCurrency(selectedOrder.price, currency)}</Text>
            </Group>
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Current Price</Text>
              <Text fw={600}>{formatCurrency(selectedOrder.currentPrice ?? selectedOrder.price, currency)}</Text>
            </Group>
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>P/L</Text>
              <Text fw={600} c={(selectedOrder.pnl ?? 0) > 0 ? "green" : (selectedOrder.pnl ?? 0) < 0 ? "red" : "gray"}>
                {formatCurrency(selectedOrder.pnl ?? 0, currency)}
              </Text>
            </Group>
            <Divider my={4} />
            <Group justify={"space-between"}>
              <Text c={"dimmed"}>Net Value</Text>
              <Text fw={700}>
                {formatCurrency(selectedOrder.netValue ?? selectedOrder.price * selectedOrder.quantity + (selectedOrder.pnl ?? 0), currency)}
              </Text>
            </Group>
          </Stack>
        )}
      </ConfirmationModal>
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
