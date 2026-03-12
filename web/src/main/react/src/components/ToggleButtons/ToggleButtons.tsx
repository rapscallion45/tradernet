import { ReactNode } from "react"
import { ButtonGroup, Tooltip } from "@mantine/core"
import { Button } from "components/Button/Button"
import classes from "./ToggleButtons.module.css"

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
    {options.map((option, index) => {
      const isFirst = index === 0
      const isLast = index === options.length - 1
      const wrapClass = `${classes.buttonWrap} ${isFirst ? classes.buttonFirst : ""} ${isLast ? classes.buttonLast : ""}`.trim()

      return (
        <Tooltip key={option.tooltip} label={option.tooltip}>
          <span className={wrapClass}>
            <Button
              size="xs"
              variant={current === option.value ? "filled" : "outline"}
              leftIcon={option.label ? option.icon : null}
              onClick={() => setCurrent(option.value)}
              disabled={current === option.value}
              aria-label={option.tooltip}>
              {option.label ?? option.icon}
            </Button>
          </span>
        </Tooltip>
      )
    })}
  </ButtonGroup>
)

export default ToggleButtons
