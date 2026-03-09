import { ReactNode } from "react"
import { ButtonGroup, Tooltip } from "@mantine/core"
import { Button } from "components/Button/Button"

export type ToggleButtonOption<T> = {
  value: T
  label?: string
  icon: ReactNode
  tooltip: string
}

type ToggleButtonsProps<T> = {
  current: T
  options: ToggleButtonOption<T>[]
  setCurrent: (value: T) => void
}

const ToggleButtons = <T = string,>({ current, options, setCurrent }: ToggleButtonsProps<T>) => (
  <ButtonGroup>
    {options.map((option) => (
      <Tooltip key={option.tooltip} label={option.tooltip}>
        <span>
          <Button
            size="xs"
            variant={current === option.value ? "filled" : "outline"}
            leftIcon={option.icon}
            onClick={() => setCurrent(option.value)}
            disabled={current === option.value}
            aria-label={option.tooltip}>
            {option.label}
          </Button>
        </span>
      </Tooltip>
    ))}
  </ButtonGroup>
)

export default ToggleButtons
