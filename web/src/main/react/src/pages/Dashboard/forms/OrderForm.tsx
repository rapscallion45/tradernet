import { FC } from "react"
import { useForm, Controller } from "react-hook-form"
import { TextInput, NumberInput, Button, Box } from "@mantine/core"
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
    <Box style={{ maxWidth: 400 }} mx={"auto"}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <TextInput label={"Symbol"} {...register("symbol")} required />
        <Controller
          name="quantity"
          control={control}
          rules={{ required: true, min: 1 }}
          render={({ field }) => (
            <NumberInput
              label="Quantity"
              {...field} // field contains value, onChange, onBlur, ref
              min={1}
            />
          )}
        />
        <Button type={"submit"} mt={"md"}>
          Submit Order
        </Button>
      </form>
    </Box>
  )
}

export default OrderForm
