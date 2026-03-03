import { FC } from "react"
import { useForm, Controller } from "react-hook-form"
import { TextInput, NumberInput, Button, Group, Stack, Select } from "@mantine/core"
import { OrderData } from "api/types"
import { ORDER_SIDES } from "api/Orders"

type OrderFormProps = {
  onSubmit: (data: OrderData) => void
  loading?: boolean
}

const OrderForm: FC<OrderFormProps> = ({ onSubmit, loading = false }) => {
  const { register, handleSubmit, control } = useForm<OrderData>({
    defaultValues: {
      side: "BUY",
      quantity: 1,
    },
  })

  return (
    <Stack>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Group grow>
          <TextInput label={"Symbol"} {...register("symbol")} required />
          <Controller
            name={"side"}
            control={control}
            rules={{ required: true }}
            render={({ field }) => (
              <Select
                required
                label={"Side"}
                data={ORDER_SIDES}
                value={field.value}
                onChange={(value) => field.onChange(value ?? "BUY")}
              />
            )}
          />
        </Group>
        <Group grow mt={"sm"}>
          <Controller
            name={"quantity"}
            control={control}
            rules={{ required: true, min: 0.000001 }}
            render={({ field }) => <NumberInput required label={"Quantity"} {...field} min={0.000001} decimalScale={6} />}
          />
          <Controller
            name={"price"}
            control={control}
            rules={{ required: true, min: 0.000001 }}
            render={({ field }) => <NumberInput required label={"Price"} {...field} min={0.000001} decimalScale={6} />}
          />
        </Group>
        <Button type={"submit"} mt={"md"} loading={loading}>
          Submit Order
        </Button>
      </form>
    </Stack>
  )
}

export default OrderForm
