import { ReactNode, FC, ReactElement } from "react"
import { Group, Modal, ButtonProps, MantineSize } from "@mantine/core"
import { Button } from "../Button/Button"
import classes from "./BaseModal.module.css"
import { wrapNodeList } from "utils/nodes"

export type CommonModalProps = {
  title: string
  opened: boolean
  size?: MantineSize | "auto"
}

export type BaseModalProps = CommonModalProps & {
  children: ReactNode
  onClose: () => void
  footer?: ReactElement<ButtonProps, typeof Button>[]
}

/**
 * A basic modal component to be used as a foundation for building other modals from.
 * This modal is not intended to be used exclusively inside a page.
 * @param title The title of the modal which displays in the header of the modal.
 * @param opened Whether the modal is open or not.
 * @param onClose An onClose function for conditional logic.
 * @param children The main content of the modal.
 * @param footer An optional alternative to the Close button. Use to pass in custom buttons such as Confirm & Cancel. Ensure type of passed in elements is of type Button and nothing else.
 * @param size Determines the width of the modal. Defaults to "md"
 */
export const BaseModal: FC<BaseModalProps> = ({ title, opened, onClose, children, footer, size = "md" }) => {
  return (
    <Modal.Root opened={opened} onClose={onClose} classNames={classes} size={size} data-testid={`${title}-modal`} centered>
      <Modal.Overlay />
      <Modal.Content>
        <Modal.Header>
          <Modal.Title role={"heading"}>{title}</Modal.Title>
          <Modal.CloseButton aria-label="Close modal" />
        </Modal.Header>
        <Modal.Body>{children}</Modal.Body>
        {/*Modal Footer*/}
        <Group className={classes.footer} justify={"end"} data-testid={"modal-footer"}>
          {footer ? wrapNodeList(footer) : <Button onClick={onClose}>Close</Button>}
        </Group>
      </Modal.Content>
    </Modal.Root>
  )
}
