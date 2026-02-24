import { FC } from "react"
import { useForm, Controller } from "react-hook-form"
import { TextInput, NumberInput, Button, Group, Stack } from "@mantine/core"
import { OrderData } from "api/types"

/**
 * Order Form props
 * @param onSubmit - submission handler
 */
type OrderFormProps = {
  onSubmit: (data: OrderData) => void
}

/**
 * Order Form component
 */
const OrderForm: FC<OrderFormProps> = ({ onSubmit }) => {
  const { register, handleSubmit, control } = useForm<OrderData>()

  return (
    <Stack>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Group grow>
          <TextInput label={"Symbol"} {...register("symbol")} required />
          <Controller
            name={"quantity"}
            control={control}
            rules={{ required: true, min: 1 }}
            render={({ field }) => <NumberInput required label={"Quantity"} {...field} min={1} />}
          />
        </Group>
        <Button type={"submit"} mt={"md"}>
          Submit Order
        </Button>
      </form>
    </Stack>
  )
}

export default OrderForm
