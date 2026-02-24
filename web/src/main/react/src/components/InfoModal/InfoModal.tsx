import { BaseModal, CommonModalProps } from "../BaseModal/BaseModal"
import { FC, ReactNode } from "react"
import { Text } from "@mantine/core"

export type BasicModalProps = CommonModalProps & {
  message?: string
  children: ReactNode
  onClose: () => void
}

/**
 * A basic modal component for displaying information to the user.
 * Extends all props of the Base Modal except "footer" and adds message prop.
 * @param message An optional string to display a simple message. If present, will display as child of modal in a mantine Text component.
 * @param children The main content of the modal.
 * @param rest Props extended from CommonModalProps & BaseModalProps. Title, Opened, children, onClose, footer, size.
 */

export const InfoModal: FC<BasicModalProps> = ({ message, children, ...rest }) => {
  return <BaseModal {...rest}>{message ? <Text>{message}</Text> : children}</BaseModal>
}
