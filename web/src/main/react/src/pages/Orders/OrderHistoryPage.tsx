import { FC } from "react"
import { Badge, Group, Loader, Paper, Stack, Table, Text } from "@mantine/core"
import { Title } from "components/Title/Title"
import PageHeader from "components/layout/PageHeader/PageHeader"
import { SectionHeading } from "components/SectionHeading/SectionHeading"
import { useOrders } from "hooks/useOrders"

const OrderHistoryPage: FC = () => {
  const { data: orders = [], isLoading } = useOrders()

  return (
    <Stack gap={"xl"}>
      <PageHeader title={<Title>Order History</Title>} description={"Track your orders and their current P/L performance."} />

      <Stack>
        <SectionHeading>ORDERS</SectionHeading>
        <Paper p={"md"} withBorder>
          {isLoading ? (
            <Group justify={"center"} py={"md"}>
              <Loader size={"sm"} />
            </Group>
          ) : (
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Created</Table.Th>
                  <Table.Th>Symbol</Table.Th>
                  <Table.Th>Side</Table.Th>
                  <Table.Th ta={"right"}>Qty</Table.Th>
                  <Table.Th ta={"right"}>Entry</Table.Th>
                  <Table.Th ta={"right"}>Current</Table.Th>
                  <Table.Th ta={"right"}>P/L</Table.Th>
                  <Table.Th ta={"right"}>P/L %</Table.Th>
                  <Table.Th>Timing</Table.Th>
                  <Table.Th>Status</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {orders.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={10}>
                      <Text size={"sm"} c={"dimmed"} ta={"center"}>
                        No orders yet.
                      </Text>
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  orders.map((order) => {
                    const pnl = order.pnl ?? 0
                    const pnlPercent = order.pnlPercent ?? 0
                    const pnlColor = pnl > 0 ? "green" : pnl < 0 ? "red" : "gray"

                    return (
                      <Table.Tr key={`${order.orderId}-${order.createdAt}`}>
                        <Table.Td>{new Date(order.createdAt).toLocaleString()}</Table.Td>
                        <Table.Td>{order.symbol}</Table.Td>
                        <Table.Td>{order.side}</Table.Td>
                        <Table.Td ta={"right"}>{order.quantity.toFixed(4)}</Table.Td>
                        <Table.Td ta={"right"}>{order.price.toFixed(4)}</Table.Td>
                        <Table.Td ta={"right"}>{(order.currentPrice ?? order.price).toFixed(4)}</Table.Td>
                        <Table.Td ta={"right"} c={pnlColor} fw={600}>
                          {pnl.toFixed(4)}
                        </Table.Td>
                        <Table.Td ta={"right"} c={pnlColor} fw={600}>
                          {pnlPercent.toFixed(2)}%
                        </Table.Td>
                        <Table.Td>
                          <Badge color={order.timing === "GOOD" ? "green" : order.timing === "BAD" ? "red" : "gray"} variant={"light"}>
                            {order.timing ?? "NEUTRAL"}
                          </Badge>
                        </Table.Td>
                        <Table.Td>
                          <Badge variant={"outline"}>{order.status}</Badge>
                        </Table.Td>
                      </Table.Tr>
                    )
                  })
                )}
              </Table.Tbody>
            </Table>
          )}
        </Paper>
      </Stack>
    </Stack>
  )
}

export default OrderHistoryPage
