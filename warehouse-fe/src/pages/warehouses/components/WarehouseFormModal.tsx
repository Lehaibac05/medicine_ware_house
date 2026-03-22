import { Form, Input } from "antd";
import { useEffect } from "react";
import BaseModal from "../../../components/base/BaseModal";

export type WarehouseFormValues = {
  name: string;
  location: string;
  description: string;
};

type WarehouseFormModalProps = {
  open: boolean;
  mode: "create" | "edit";
  loading?: boolean;
  initialValues?: Partial<WarehouseFormValues>;
  onCancel: () => void;
  onSubmit: (values: WarehouseFormValues) => Promise<void> | void;
};

const defaultValues: WarehouseFormValues = {
  name: "",
  location: "",
  description: "",
};

function WarehouseFormModal({
  open,
  mode,
  loading = false,
  initialValues,
  onCancel,
  onSubmit,
}: WarehouseFormModalProps) {
  const [form] = Form.useForm<WarehouseFormValues>();

  useEffect(() => {
    if (!open) {
      return;
    }

    form.setFieldsValue({
      ...defaultValues,
      ...initialValues,
    });
  }, [open, form, initialValues]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit({
      ...values,
      name: values.name.trim(),
      location: values.location.trim(),
      description: values.description.trim(),
    });
  };

  return (
    <BaseModal
      open={open}
      title={mode === "create" ? "Thêm kho" : "Cập nhật kho"}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "Tạo" : "Cập nhật"}
      confirmLoading={loading}
      width={680}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="Tên kho"
          rules={[
            { required: true, message: "Vui lòng nhập tên kho." },
            { max: 255, message: "Tên kho quá dài." },
          ]}
        >
          <Input placeholder="Ví dụ: Kho trung tâm Quận 1" />
        </Form.Item>

        <Form.Item
          name="location"
          label="Vị trí"
          rules={[
            { required: true, message: "Vui lòng nhập vị trí kho." },
            { max: 255, message: "Vị trí kho quá dài." },
          ]}
        >
          <Input placeholder="Ví dụ: 123 Nguyễn Huệ, Quận 1" />
        </Form.Item>

        <Form.Item
          name="description"
          label="Mô tả"
          rules={[{ max: 1000, message: "Mô tả quá dài." }]}
        >
          <Input.TextArea
            rows={4}
            placeholder="Mô tả công năng, lưu ý bảo quản hoặc phạm vi phụ trách..."
            showCount
            maxLength={1000}
          />
        </Form.Item>
      </Form>
    </BaseModal>
  );
}

export default WarehouseFormModal;
