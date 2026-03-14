import { Modal } from "antd";
import type { ReactNode } from "react";

type BaseModalProps = {
  open: boolean;
  title: ReactNode;
  onOk?: () => void;
  onCancel: () => void;
  children: ReactNode;

  okText?: string;
  cancelText?: string;

  confirmLoading?: boolean;
  width?: number | string;
  destroyOnClose?: boolean;
  footer?: ReactNode;
};

function BaseModal({
  open,
  title,
  onOk,
  onCancel,
  children,
  okText = "Save",
  cancelText = "Cancel",
  confirmLoading = false,
  width = 600,
  destroyOnClose = true,
  footer,
}: BaseModalProps) {
  return (
    <Modal
      open={open}
      title={title}
      onOk={onOk}
      onCancel={onCancel}
      okText={okText}
      cancelText={cancelText}
      confirmLoading={confirmLoading}
      width={width}
      destroyOnClose={destroyOnClose}
      footer={footer}
    >
      {children}
    </Modal>
  );
}

export default BaseModal;