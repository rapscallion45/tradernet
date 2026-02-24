import { FC, ReactNode } from "react"
import { Alert } from "@mantine/core"
import { AppVariant } from "global/types"

export type MessageBoxProps = {
  title?: string
  icon?: ReactNode
  variant?: AppVariant
  children: ReactNode
}

/**
 * MessageBox component to display a message to the user. Designed to take up full width so it grabs attention, so ensure that lines of text are not too long.
 *
 * @param title Title to display at the top of the message box. Optional.
 * @param icon Icon to display next to the title. Defaults to "circle-info".
 * @param variant Variant of the message box. Defaults to "default", which should be preferred in most cases.
 * @param children The content of the message box. Can be a simple string or more complex JSX.
 */
export const MessageBox: FC<MessageBoxProps> = ({ title, icon = "circle-info", variant = "default", children }) => (
  <Alert variant={variant} title={title} icon={icon}>
    {children}
  </Alert>
)
