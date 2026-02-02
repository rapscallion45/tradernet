import { AriaAttributes, forwardRef, ForwardRefExoticComponent, ReactNode } from "react"
import { Button as MantineButton } from "@mantine/core"
import { AppSize, AppVariant } from "global/types"
import classes from "./Button.module.css"

export type ButtonProps = AriaAttributes & {
  variant?: AppVariant
  size?: AppSize
  leftIcon?: ReactNode
  rightIcon?: ReactNode
  onClick?: () => void
  onMouseEnter?: () => void
  disabled?: boolean
  children?: ReactNode
  type?: "button" | "submit" | "reset"
  fullWidth?: boolean
  loading?: boolean
  adaptive?: boolean
}

/**
 * Button component with **three variants** and left and right icons.
 *
 * This button is **deliberately very restrictive** in its props to ensure consistency across the application.
 * There are three main variants, which render a button as a solid purple block, an outlined purple block, or a text link.
 *
 * Standard controls such as `size`, `onClick` and `disabled` are also available.
 */
const Button: ForwardRefExoticComponent<ButtonProps> = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = "filled", size = "md", leftIcon, rightIcon, onClick, onMouseEnter, disabled, type = "button", fullWidth, loading, children, ...rest }, ref) => (
    <MantineButton
      ref={ref}
      classNames={classes}
      variant={variant}
      size={size}
      type={type}
      leftSection={!!leftIcon}
      rightSection={!!rightIcon}
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      disabled={disabled}
      fullWidth={fullWidth}
      loading={loading}
      {...rest}>
      {children}
    </MantineButton>
  ),
)

export default Button
