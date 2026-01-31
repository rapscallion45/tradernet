import { AriaAttributes, forwardRef, ForwardRefExoticComponent, ReactNode } from "react"
import { ActionIcon as MantineActionIcon, Tooltip } from "@mantine/core"
import { AppSize, AppVariant } from "global/types"

type ActionIconProps = AriaAttributes & {
  icon: ReactNode
  onClick?: () => void
  size?: AppSize
  matchFormSize?: boolean
  variant?: AppVariant
  disabled?: boolean
  tooltip?: string
}

/**
 * ActionIcon component to display an icon with an optional click handler.
 *
 * @param icon The icon to display
 * @param onClick The click handler
 * @param size The size of the icon. Defaults to "lg"
 * @param matchFormSize Whether the icon should match the size of a form element. Defaults to false
 * @param color The color of the icon. Defaults to "secondary"
 * @param variant The variant of the icon. Defaults to "outline"
 * @param disabled Whether the icon should be disabled. Defaults to false
 * @param tooltip The tooltip to display on hover
 */
const ActionIcon: ForwardRefExoticComponent<ActionIconProps> = forwardRef<HTMLButtonElement, ActionIconProps>(
  ({ icon, onClick, size = "lg", matchFormSize, variant = "outline", disabled, tooltip, ...rest }, ref) => {
    return (
      <Tooltip label={tooltip} disabled={!tooltip}>
        <MantineActionIcon ref={ref} variant={variant} size={matchFormSize ? `input-${size}` : size} onClick={onClick} disabled={disabled} {...rest}>
          {icon}
        </MantineActionIcon>
      </Tooltip>
    )
  },
)

export default ActionIcon
