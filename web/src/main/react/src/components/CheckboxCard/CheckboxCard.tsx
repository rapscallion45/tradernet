import { Checkbox, Group, Text } from "@mantine/core"
import { FC, forwardRef, ReactNode, useEffect, useState } from "react"
import classes from "./CheckboxCard.module.css"
import { IconCheck, IconLineDashed } from "@tabler/icons-react"

export type CheckboxCardProps = {
  label: string
  description?: string
  checked?: boolean
  externalChecked?: boolean
  externalCheckedIcon?: ReactNode
  value?: string
  defaultChecked?: boolean
  onChange?: (value: boolean) => void
  disabled?: boolean
  modified?: boolean
  stacked?: boolean
}

/**
 * CheckboxCard component that performs a simple toggle action.
 *
 * Now *does* contain an indeterminate state!
 *
 * @param label The label to display next to the checkbox.
 * @param description The description to display below the label.
 * @param checked Whether the checkbox is checked or not. Use this in controlled mode.
 * @param externalChecked Whether the checkbox is 'forced on' by some external state.
 * @param externalCheckedIcon The icon to display when the checkbox is externally checked.
 * @param value The value of the checkbox input.
 * @param defaultChecked The default checked state of the checkbox. Use this in uncontrolled mode.
 * @param onChange The function to call when the checkbox is toggled.
 * @param disabled Whether the checkbox is disabled or not.
 * @param modified Whether the card is in a modified state.
 */
export const CheckboxCard: FC<CheckboxCardProps> = forwardRef<HTMLInputElement, CheckboxCardProps>(
  (
    { checked = false, externalChecked = false, externalCheckedIcon = "minus", defaultChecked, value, label, description, disabled, modified, onChange },
    ref,
  ) => {
    const [isChecked, setIsChecked] = useState(defaultChecked ?? checked)

    useEffect(() => {
      setIsChecked(checked)
    }, [checked])

    // Unique name to link the label to the checkbox
    const name = `${label}-CheckboxCard`

    const handleChange = () => {
      setIsChecked((current) => {
        onChange && onChange(!current)
        return !current
      })
    }

    return (
      <Checkbox.Card
        mod={{ modified }}
        className={classes.root}
        checked={isChecked || externalChecked}
        value={value}
        onClick={handleChange}
        disabled={disabled}
        aria-label={name}>
        <Group wrap="nowrap" align="center">
          <Checkbox
            value={value}
            indeterminate={externalChecked}
            icon={({ indeterminate, ...others }) => (checked ? <IconCheck /> : indeterminate ? <IconLineDashed /> : <IconCheck />)}
            ref={ref}
            checked={isChecked}
            onClick={(event) => {
              event.stopPropagation()
            }}
            onChange={handleChange}
            disabled={disabled}
          />
          <div>
            <label htmlFor={name} className={classes.label}>
              {label}
            </label>
            <Text className={classes.description}>{description}</Text>
          </div>
        </Group>
      </Checkbox.Card>
    )
  },
)
