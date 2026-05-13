import { FC, useEffect, useState } from "react"
import { useForm, Controller } from "react-hook-form"
import { Text, NumberInput, Button, Group, Stack, Select } from "@mantine/core"
import { useQuery } from "@tanstack/react-query"
import { OrderData } from "api/types"
import { ORDER_SIDES } from "api/Orders"
import { getRestClient } from "api/RestClient"
import { DEFAULT_CHART_SYMBOL, QueryClientKeys } from "global/constants"
import { formatCurrency } from "utils/intl"
import { useMarketSymbols } from "hooks/useMarketSymbols"
import { useCurrencyPreference } from "hooks/useCurrencyPreference"

type OrderFormProps = {
  onSubmit: (data: OrderData) => void
  loading?: boolean
}

const toNumeric = (value: string | number | null | undefined): number => {
  if (typeof value === "number") return Number.isFinite(value) ? value : 0
  if (typeof value === "string") {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

const OrderForm: FC<OrderFormProps> = ({ onSubmit, loading = false }) => {
  const { handleSubmit, control, watch, setValue } = useForm<OrderData>({
    defaultValues: {
      symbol: DEFAULT_CHART_SYMBOL,
      side: "BUY",
      quantity: 1,
      price: 0,
    },
  })

  const symbol = watch("symbol") || DEFAULT_CHART_SYMBOL
  const quantityValue = watch("quantity")
  const priceValue = watch("price")
  const [lastEdited, setLastEdited] = useState<"quantity" | "price">("quantity")
  const { currency, setCurrency, currencyOptions } = useCurrencyPreference()

  const { data: currentUnitPrice = 0 } = useQuery({
    queryKey: [QueryClientKeys.MarketBars, symbol, currency],
    queryFn: async () => {
      const bars = await getRestClient().marketResource.getBars(symbol, "1S", 1, currency)
      return bars[0]?.close ?? 0
    },
    refetchInterval: 3000,
  })


  const { data: currentUnitPriceRaw = 0 } = useQuery({
    queryKey: [QueryClientKeys.MarketBars, symbol, "RAW"],
    queryFn: async () => {
      const bars = await getRestClient().marketResource.getBars(symbol, "1S", 1)
      return bars[0]?.close ?? 0
    },
    refetchInterval: 3000,
  })

  useEffect(() => {
    const quantity = toNumeric(quantityValue)
    const amount = toNumeric(priceValue)

    if (currentUnitPrice <= 0) {
      return
    }

    if (lastEdited === "quantity") {
      const nextAmount = quantity * currentUnitPrice
      if (Number.isFinite(nextAmount)) {
        const roundedAmount = Number(nextAmount.toFixed(6))
        if (Math.abs(roundedAmount - amount) > 0.0000001) {
          setValue("price", roundedAmount, { shouldDirty: true })
        }
      }
      return
    }

    const nextQuantity = amount / currentUnitPrice
    if (Number.isFinite(nextQuantity)) {
      const roundedQuantity = Number(nextQuantity.toFixed(6))
      if (Math.abs(roundedQuantity - quantity) > 0.0000001) {
        setValue("quantity", roundedQuantity, { shouldDirty: true })
      }
    }
  }, [currentUnitPrice, lastEdited, priceValue, quantityValue, setValue])

  const { data: symbolOptions = [DEFAULT_CHART_SYMBOL] } = useMarketSymbols()

  useEffect(() => {
    if (!symbolOptions.includes(symbol)) {
      setValue("symbol", symbolOptions[0] ?? DEFAULT_CHART_SYMBOL, { shouldDirty: true })
    }
  }, [setValue, symbol, symbolOptions])

  const handleOrderSubmit = (data: OrderData) => {
    const resolvedUnitPrice = currentUnitPriceRaw > 0 ? currentUnitPriceRaw : data.quantity > 0 ? data.price / data.quantity : 0

    onSubmit({
      ...data,
      symbol,
      quantity: toNumeric(data.quantity),
      price: Number(resolvedUnitPrice.toFixed(6)),
    })
  }

  return (
    <Stack>
      <form onSubmit={handleSubmit(handleOrderSubmit)}>
        <Group grow>
          <Select label={"Currency"} data={currencyOptions} value={currency} onChange={(value) => setCurrency(value ?? currency)} />
          <Controller
            name={"symbol"}
            control={control}
            rules={{ required: true }}
            render={({ field }) => (
              <Select required label={"Symbol"} data={symbolOptions} value={field.value} onChange={(value) => field.onChange(value ?? DEFAULT_CHART_SYMBOL)} />
            )}
          />
          <Controller
            name={"side"}
            control={control}
            rules={{ required: true }}
            render={({ field }) => (
              <Select required label={"Position"} data={ORDER_SIDES} value={field.value} onChange={(value) => field.onChange(value ?? "BUY")} />
            )}
          />
        </Group>
        <Group grow mt={"sm"}>
          <Controller
            name={"quantity"}
            control={control}
            rules={{ required: true, min: 0.000001 }}
            render={({ field }) => (
              <NumberInput
                required
                label={"Quantity"}
                value={field.value}
                onChange={(value) => {
                  setLastEdited("quantity")
                  field.onChange(toNumeric(value))
                }}
                min={0.000001}
                decimalScale={6}
              />
            )}
          />
          <Controller
            name={"price"}
            control={control}
            rules={{ required: true, min: 0.000001 }}
            render={({ field }) => (
              <NumberInput
                required
                label={"Price"}
                value={field.value}
                onChange={(value) => {
                  setLastEdited("price")
                  field.onChange(toNumeric(value))
                }}
                min={0.000001}
                decimalScale={6}
              />
            )}
          />
        </Group>
        <Text size={"xs"} c={"dimmed"} mt={"xs"}>
          Current unit price: {currentUnitPrice > 0 ? formatCurrency(currentUnitPrice, currency) : "Loading..."}
        </Text>
        <Button type={"submit"} mt={"md"} loading={loading}>
          Submit Order
        </Button>
      </form>
    </Stack>
  )
}

export default OrderForm
