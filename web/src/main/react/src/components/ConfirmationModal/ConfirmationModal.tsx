import { FC, ReactNode } from "react"
import { Text } from "@mantine/core"
import { Button } from "../Button/Button"
import { BaseModal, CommonModalProps } from "../BaseModal/BaseModal"

type ConfirmationModalProps = CommonModalProps &
  ({ message?: string; children?: never } | { children: ReactNode; message?: never }) & {
    onConfirm: () => void
    onCancel: () => void
    disableConfirm?: boolean
    disableCancel?: boolean
    loading?: boolean
    confirmTextOverride?: string
  }

/**
 * A modal component to be used when the content within the Children prop can be acted upon.
 * Extends all props of the Base Modal except "footer" and adds props unique to itself.
 * @param message An optional string to display a simple message. If present, will display as child of modal in a mantine Text component.
 * @param onConfirm A function for conditional logic to run onClick of the Confirm button.
 * @param onCancel  A function for conditional logic to run onClick of the Cancel button. Replaces onClose.
 * @param disableConfirm A boolean value which dictates the state of the confirm button.
 * @param loading An optional boolean used to display a loading spinner inside confirm button.
 * @param confirmTextOveride An optional string value to change the text on the confirm button to something more relevant to the children content.
 */
export const ConfirmationModal: FC<ConfirmationModalProps> = ({
  message,
  onConfirm,
  onCancel,
  disableConfirm = false,
  loading = false,
  confirmTextOverride = "Confirm",
  children,
  ...rest
}) => {
  const footer = [
    <Button variant={"subtle"} onClick={onCancel}>
      Cancel
    </Button>,
    <Button variant={"filled"} onClick={onConfirm} disabled={disableConfirm} loading={loading}>
      {confirmTextOverride ? confirmTextOverride : "Confirm"}
    </Button>,
  ]

  return (
    <BaseModal {...rest} onClose={onCancel} footer={footer}>
      {message ? <Text>{message}</Text> : children}
    </BaseModal>
  )
}
