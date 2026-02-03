import { Box, Combobox, InputBase, useCombobox } from "@mantine/core"
import { AppInputVariant, AppLength, AppSize } from "global/types"
import { useState } from "react"
import { IconCaretDown } from "@tabler/icons-react"
import classes from "./Dropdown.module.css"

export type Option = {
  value: string
  label: string
}

export type DropdownProps<T extends Option> = {
  name?: string
  label?: string
  description?: string
  value?: T
  onChange?: (value: T | undefined) => void | undefined
  data: T[]
  size?: AppSize
  width?: AppLength
  variant?: AppInputVariant
}

const Dropdown = <T extends Option>({ name, label, description, value, onChange, data, size = "sm", width, variant = "default" }: DropdownProps<T>) => {
  console.log("value", value)

  const combobox = useCombobox()
  const [theValue, setTheValue] = useState<string | undefined>(value?.value)
  const selectedOption = data.find((item) => item.value === theValue)

  return (
    <Box w={width}>
      <Combobox
        variant={variant}
        size={size}
        store={combobox}
        onOptionSubmit={(val) => {
          console.log("val", val)
          console.log("theValue", theValue)
          // Either sets the value or clears it
          if (val === theValue) {
            // value was clicked again, clear it all
            setTheValue(undefined)
            onChange?.(undefined)
          } else {
            // value was changed, set it
            setTheValue(val)
            onChange?.(data.find((item) => item.value === val) as T)
          }
          combobox.closeDropdown()
        }}>
        <Combobox.Target>
          <InputBase
            variant={variant}
            classNames={classes}
            name={name}
            label={label}
            description={description}
            size={size}
            component={"button"}
            type={"button"}
            pointer
            rightSection={<IconCaretDown />}
            rightSectionPointerEvents={"none"}
            onClick={() => combobox.toggleDropdown()}>
            {selectedOption?.label}
          </InputBase>
        </Combobox.Target>
        <Combobox.Dropdown>
          <Combobox.Options>
            {data.map((item, index) => (
              <Combobox.Option value={item.value} key={index} className={classes.option}>
                {item.label}
              </Combobox.Option>
            ))}
          </Combobox.Options>
        </Combobox.Dropdown>
      </Combobox>
    </Box>
  )
}

export default Dropdown
